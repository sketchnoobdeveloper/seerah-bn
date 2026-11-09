# seerah-bn — আর-রাহীকুল মাখতুম (বাংলা)

Complete Bangla **Ar-Raheeq Al-Makhtum** (আর-রাহীকুল মাখতুম) in a clean,
chapter-based JSON format for Android, web and offline readers.

- **Book:** আর-রাহীকুল মাখতুম
- **Author:** সফিউর রহমান মুবারকপুরী (Safiur Rahman Mubarakpuri)
- **Translator:** খাদিজা আখতার রেজায়ী
- **Language:** Bengali (`bn`)
- **Chapters:** 50
- **Source:** [IslamHouse Bangla PDF](https://d1.islamhouse.com/data/bn/ih_books/single/bn_ar_raheequl_makhtoom.pdf)
- **Edition:** 508-page IslamHouse scan; text is OCR-derived and cleaned into
  plain UTF-8 paragraphs. It is not a new translation or a scholarly review.

## Repository layout

```text
seerah-bn/
├── README.md
├── LICENSE
├── data/
│   ├── seerah.json              # full book: meta + 50 chapters + content blocks
│   ├── seerah_index.json        # meta + chapter titles only (fast loading)
│   ├── manifest.json            # sizes and SHA-256 for every generated file
│   ├── source/
│   │   └── raheeq_ocr.txt       # cached OCR text used to build this edition
│   └── chapters/
│       ├── chapter_01.json
│       └── ... chapter_50.json
├── txt/
│   ├── seerah_full.txt
│   └── chapters/
│       ├── chapter_01.txt
│       └── ... chapter_50.txt
├── scripts/
│   ├── pdf_to_json.py
│   └── highlights_bn.py       # short reading cues for every chapter
└── android/
    ├── README.md
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── java/com/snd/assaheed/seerah/
        └── res/layout/
```

## Data format

`data/seerah.json`:

```json
{
  "meta": {
    "title": "আর-রাহীকুল মাখতুম",
    "title_en": "Ar-Raheeq Al-Makhtum",
    "author": "সফিউর রহমান মুবারকপুরী",
    "translator": "খাদিজা আখতার রেজায়ী",
    "language": "bn",
    "total_chapters": 50,
    "source": "islamhouse.com"
  },
  "chapters": [
    {
      "id": 1,
      "title": "আরবের ভৌগোলিক পরিচয় ও জাতিসমূহ",
      "slug": "chapter-01",
      "period": "প্রাক-ইসলামি আরব",
      "summary": "আরব উপদ্বীপের অবস্থান, ভৌগোলিক বৈশিষ্ট্য ...",
      "highlights": {
        "subtitle": "এক মরুভূমি, তিন জাতি, এক মহান আগমন",
        "lesson": "ভৌগোলিক পরিবেশ মানুষ ও জাতির চরিত্র গড়ে তোলে।",
        "fun": "আরব জাতিকে বারেবা, আবেরা ও মোস্তারেবা—তিন ভাগে ভাগ করা হয়।",
        "practice": "নিজের পরিবেশ ও ইতিহাস জানুন, দাওয়াতের প্রেক্ষাপট বুঝুন।"
      },
      "char_count": 38767,
      "content": [
        { "type": "heading", "text": "আরবের ভৌগোলিক পরিচয় ও জাতিসমূহ" },
        { "type": "paragraph", "text": "সীরাতে নববী প্রকৃতপক্ষে ..." }
      ]
    }
  ]
}
```

Each `data/chapters/chapter_XX.json` is one chapter object (the same object as
inside `chapters`). `data/seerah_index.json` contains only `meta` and the
chapter list, so an app can show the table of contents without downloading the
full 3.2 MB book.

### Reading cues

Every chapter has a `highlights` object with four short editorial fields:

- `subtitle` — a one-line hook for the chapter card
- `lesson` — the main lesson (`শিক্ষা`)
- `fun` — an interesting point (`মজার তথ্য`)
- `practice` — a practical takeaway (`প্রয়োগ`)

These are concise summaries written for the app, not a substitute for the full
text or a scholarly commentary. They live in `scripts/highlights_bn.py` and are
merged by `pdf_to_json.py`, so a rebuild keeps them.

## Raw GitHub CDN URLs

Replace `main` with a tag or commit SHA for immutable content.

```text
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/seerah_index.json
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/seerah.json
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/chapters/chapter_01.json
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/chapters/chapter_50.json
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/txt/seerah_full.txt
https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/txt/chapters/chapter_01.txt
```

GitHub also serves the same files through jsDelivr:

```text
https://cdn.jsdelivr.net/gh/sketchnoobdeveloper/seerah-bn@main/data/seerah_index.json
https://cdn.jsdelivr.net/gh/sketchnoobdeveloper/seerah-bn@main/data/chapters/chapter_01.json
```

## Usage

### Android (HttpURLConnection + org.json)

```java
final String indexUrl =
    "https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/seerah_index.json";

new Thread(new Runnable() {
    @Override public void run() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(indexUrl).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/json");
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            input.close();
            JSONObject index = new JSONObject(output.toString("UTF-8"));
            JSONArray chapters = index.getJSONArray("chapters");
            // post chapters to the UI thread with a Handler
        } catch (Exception error) {
            // show a friendly offline/error state
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}).start();
```

The complete Java reference implementation is in the companion `android/`
directory of this repository (see its README).

### Web

```js
const index = await fetch(
  "https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/seerah_index.json"
).then((response) => response.json());

const chapter = await fetch(
  `https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/data/chapters/chapter_${String(index.chapters[0].id).padStart(2, "0")}.json`
).then((response) => response.json());

for (const block of chapter.content) {
  console.log(block.type, block.text);
}
```

## Build from the PDF

The PDF is an image-only scan, so OCR is required. The script supports PyMuPDF
or `pdftotext` when a text layer exists, a cached OCR file, and a Tesseract
Bengali fallback.

```bash
python3 scripts/pdf_to_json.py \
  --pdf /path/to/raheeq_bn.pdf \
  --ocr-text data/source/raheeq_ocr.txt \
  --out .
```

The chapter boundary table is specific to this IslamHouse edition. The cached
OCR text is included so the generated data can be reproduced exactly. A fresh
Tesseract OCR may produce different wording and will use heading-based chapter
detection instead of the verified offsets. Reading cues are kept in
`scripts/highlights_bn.py` and are merged into every chapter automatically.

## Android module

`android/` is a standalone Java/XML reader that follows the Sketchware Pro
constraints: no lambdas, no try-with-resources, anonymous inner classes,
`Thread` + `Handler`, `HttpURLConnection` + `org.json`, `DownloadManager`, and
`minSdk 21`. It includes:

- compact premium chapter cards: number, title, subtitle, `শিক্ষা`,
  `মজার তথ্য` and `প্রয়োগ`
- online reading by default; the slim save bar disappears after the book is
  saved to the app library
- saved-copy chip with size, delete in the overflow menu
- reader with the same reading cues, previous/next, font size and progress
- full-book keyword search
- day/night layouts and Bangla typography

See [`android/README.md`](android/README.md) for the class list, build command
and drop-in integration steps. The module compiles independently; the current
As-Sahih app is Kotlin/Compose, so it is not wired into that navigation graph.

## Source, licence and review limits

- **Source credit:** IslamHouse.com, *আর-রাহীকুল মাখতুম*.
- **Content licence:** free for Islamic and educational use with source credit.
  The underlying Bengali translation's redistribution permission has not been
  independently confirmed; rights remain with the original publisher and
  translator. This repository does not claim a new licence over the translation.
- **Code licence:** MIT (see `LICENSE`).
- **Review status:** OCR-derived technical cleanup only. Some recognition
  artifacts remain, especially in chapter 3. The text has not been
  scholar-reviewed and is not a substitute for a printed critical edition.
