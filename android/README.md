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
- `DownloadManager` for the optional offline copy

The module is a normal Android application so it can be built and run on its
own, then copied into an existing app.

## Screens and classes

| File | Responsibility |
| --- | --- |
| `SeerahIndexActivity.java` | Chapter list, search entry, download button, offline badge, size, delete |
| `SeerahIndexLoader.java` | Fetch/parse `seerah_index.json` on a background thread |
| `SeerahReaderActivity.java` | Chapter reader, previous/next, font size, scroll progress |
| `SeerahChapterLoader.java` | Offline-first chapter load, per-chapter online fetch, full-book fallback |
| `SeerahBlockAdapter.java` | Lazy heading/paragraph rendering in a RecyclerView |
| `OfflineSeerahManager.java` | DownloadManager enqueue/query, validation, atomic internal copy, delete |
| `SeerahSearchActivity.java` | Full-book keyword search UI |
| `SeerahSearchManager.java` | Background search across every chapter body |
| `SeerahJson.java` | `org.json` parsing for the index and chapter schema |
| `SeerahHttp.java` | Bounded HTTPS helper with jsDelivr fallback |
| `SeerahTheme.java` | Day/night colour resolution |

Layouts are in `app/src/main/res/layout/`:

- `activity_seerah_index.xml`
- `activity_seerah_reader.xml`
- `activity_seerah_search.xml`
- `item_seerah_chapter.xml`
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
SHA-256 f6a5871c85a280dfed9e59b1c8bc7ccf86231d191122ca6e0ee86ea09335792d
```

## Offline flow

1. `অফলাইনে ডাউনলোড` enqueues `data/seerah.json` with `DownloadManager`.
2. The download goes to the app-specific external files directory; no storage
   permission is required on API 19+.
3. A 500 ms `Handler` poll reads `DownloadManager.Query` and updates the progress
   bar and percentage.
4. On success the JSON is validated with the same parser, then written to
   `getFilesDir()/seerah/seerah.json` through a `.part` file and rename.
5. `SeerahIndexLoader` and `SeerahChapterLoader` prefer that internal file, so
   the whole reader works without a network.
6. `মুছুন` removes the internal file and clears the download id.

If the download is interrupted, `DownloadManager` keeps its own partial file and
the screen offers the download button again. A failed or invalid JSON is never
promoted to the offline copy.

## Error handling

- **No internet:** the index and chapter screens show a Bangla message and a
  retry button; an existing offline copy is used first.
- **JSON parse error:** the parser exception is converted to a friendly message;
  the invalid download is discarded.
- **Large file:** online reading uses the small `chapter_XX.json` files; the full
  book is only fetched for search or as a fallback. The reader uses a
  `RecyclerView`, so only visible blocks are inflated.
- **Download interrupted:** the retry action re-enqueues the download.

## Integrating into As-Sahih

The current As-Sahih app is Kotlin/Compose. This module is deliberately
Java/XML so it can be used in Sketchware Pro or a classic Android project.

To add it to As-Sahih:

1. Copy `app/src/main/java/com/snd/assaheed/seerah/` into the app source set.
2. Copy the six layouts and two drawables; merge `colors.xml`, `strings.xml` and
   `themes.xml` without overwriting the existing warm theme names.
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
