# Seerah Android reference module

A self-contained Java/XML reader for the `seerah-bn` data. It follows the
Sketchware Pro constraints:

- Java only, no Kotlin
- no lambdas and no method references
- no try-with-resources
- anonymous inner classes only for callbacks
- `Thread` + `Handler` only (no AsyncTask, RxJava or coroutines)
- `org.json` + `HttpURLConnection`
- `minSdk 21`, `targetSdk 34`
- `DownloadManager` for the optional saved copy

The module is a normal Android application so it can be built and run on its
own, then copied into an existing app.

## Reading experience

- **Chapter cards:** chapter number, title, a short subtitle hook, the period,
  and three labelled cues — `শিক্ষা` (lesson), `মজার তথ্য` (interesting point)
  and `প্রয়োগ` (practical use). Cards are compact: each cue is one line and the
  full text appears at the top of the reader.
- **Reader:** the same cues are shown once as a premium header card, followed by
  lazily rendered headings and paragraphs, previous/next navigation, font size
  and a scroll progress bar.
- **Online by default:** the first launch reads `seerah_index.json` and then the
  small per-chapter files. Nothing has to be downloaded.
- **Library copy:** a slim bottom bar offers `সংরক্ষণ`. The app downloads the
  full book with `DownloadManager`, validates it, and stores it in internal
  storage. The bar disappears completely after a successful save; the header
  then shows a `✓ লাইব্রেরিতে সংরক্ষিত` chip and the size. Delete lives in the
  overflow menu, not in a permanent promo card.
- **Search:** full-book keyword search with snippets and chapter jump.

## Screens and classes

| File | Responsibility |
| --- | --- |
| `SeerahIndexActivity.java` | Chapter list, search entry, slim save bar, saved-copy chip, overflow menu |
| `SeerahIndexLoader.java` | Fetch/parse `seerah_index.json` on a background thread |
| `SeerahReaderActivity.java` | Chapter reader, previous/next, font size, scroll progress |
| `SeerahChapterLoader.java` | Saved-copy-first chapter load, per-chapter online fetch, full-book fallback |
| `SeerahBlockAdapter.java` | Reading-cue header plus lazy heading/paragraph rendering |
| `SeerahLibraryManager.java` | DownloadManager enqueue/query, validation, atomic internal copy, delete |
| `SeerahSearchActivity.java` | Full-book keyword search UI |
| `SeerahSearchManager.java` | Background search across every chapter body |
| `SeerahJson.java` | `org.json` parsing for the index, highlights and chapter schema |
| `SeerahHttp.java` | Bounded HTTPS helper with jsDelivr fallback |
| `SeerahHighlights.java` | Subtitle, lesson, interesting point and practical-use model |
| `SeerahTheme.java` | Day/night colour resolution |

Layouts are in `app/src/main/res/layout/`:

- `activity_seerah_index.xml`
- `activity_seerah_reader.xml`
- `activity_seerah_search.xml`
- `item_seerah_chapter.xml`
- `item_seerah_highlights.xml`
- `item_seerah_block.xml`
- `item_seerah_search_result.xml`

## Build

```bash
cd android
printf 'sdk.dir=/path/to/Android/Sdk\n' > local.properties
gradle :app:assembleDebug
```

On this build machine the cached AAPT2 binary is a shim, so the verified build
command was:

```bash
GRADLE_USER_HOME=/tmp/opencode/assaheed-test-cache \
  /opt/android-cli/gradle-8.4/bin/gradle :app:assembleDebug \
  --console=plain --no-daemon \
  -Pandroid.aapt2FromMavenOverride=/opt/aapt2-shim/aapt2
```

The generated debug APK is about 1.5 MB:

```text
SHA-256 4fec23caeb25a900c33830f763a14db006a153d91e8a604e4b0ae414176e5620
```

## Saved-copy flow

1. The reader taps `সংরক্ষণ` in the slim bottom bar.
2. `SeerahLibraryManager` enqueues `data/seerah.json` with `DownloadManager`.
3. The download goes to the app-specific external files directory; no storage
   permission is required on API 19+.
4. A 500 ms `Handler` poll reads `DownloadManager.Query` and updates the bar.
5. On success the JSON is validated with the same parser, then written to
   `getFilesDir()/seerah/seerah.json` through a `.part` file and rename.
6. The save bar is hidden, the `লাইব্রেরিতে সংরক্ষিত` chip and size appear, and
   `SeerahIndexLoader` / `SeerahChapterLoader` prefer the saved file.
7. `⋮ → সংরক্ষিত কপি মুছুন` removes the internal file and clears the download id.

If the download is interrupted, the bar returns with the `সংরক্ষণ` action. A
failed or invalid JSON is never promoted to the saved copy.

## Error handling

- **No internet:** the index and chapter screens show a Bangla message and a
  retry button; an existing saved copy is used first.
- **JSON parse error:** the parser exception is converted to a friendly message;
  the invalid download is discarded.
- **Large file:** online reading uses the small `chapter_XX.json` files; the full
  book is only fetched for search or as a fallback. The reader uses a
  `RecyclerView`, so only visible blocks are inflated.
- **Download interrupted:** the save bar reappears and re-enqueues on tap.

## Integrating into As-Sahih

The current As-Sahih app is Kotlin/Compose. This module is deliberately
Java/XML so it can be used in Sketchware Pro or a classic Android project.

To add it to As-Sahih:

1. Copy `app/src/main/java/com/snd/assaheed/seerah/` into the app source set.
2. Copy the seven layouts and three drawables; merge `colors.xml`, `strings.xml`
   and `themes.xml` without overwriting the existing warm theme names.
3. Add `SeerahIndexActivity`, `SeerahReaderActivity` and
   `SeerahSearchActivity` to `AndroidManifest.xml`; remove the `MAIN`/`LAUNCHER`
   filter from the index activity.
4. Keep `INTERNET`, `ACCESS_NETWORK_STATE` and `POST_NOTIFICATIONS`.
5. `androidx.recyclerview:recyclerview:1.3.2` is already a dependency of the
   As-Sahih app.
6. Launch it from a Compose/Kotlin screen with:

```kotlin
context.startActivity(
    Intent(context, com.snd.assaheed.seerah.SeerahIndexActivity::class.java)
)
```

The module does not depend on Compose, Hilt, Room or the app's navigation graph,
so it can be added without changing the existing Sirat reader.

## Data URLs

```text
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/seerah_index.json
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/chapters/chapter_01.json
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/seerah.json
```

jsDelivr mirrors are used as the fallback for every request.
