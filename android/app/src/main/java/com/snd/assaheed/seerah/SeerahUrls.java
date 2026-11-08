package com.snd.assaheed.seerah;

import java.util.Locale;

/** Public CDN locations for the seerah-bn data repository. */
public final class SeerahUrls {
    private static final String BASE =
            "https://raw.githubusercontent.com/sketchnoobdeveloper/seerah-bn/main/";
    private static final String JSDELIVR_BASE =
            "https://cdn.jsdelivr.net/gh/sketchnoobdeveloper/seerah-bn@main/";

    public static final String INDEX = BASE + "data/seerah_index.json";
    public static final String INDEX_JSDELIVR = JSDELIVR_BASE + "data/seerah_index.json";
    public static final String FULL = BASE + "data/seerah.json";
    public static final String FULL_JSDELIVR = JSDELIVR_BASE + "data/seerah.json";

    private SeerahUrls() {
    }

    public static String chapter(int chapterId) {
        return BASE + "data/chapters/chapter_" + pad(chapterId) + ".json";
    }

    public static String chapterJsdelivr(int chapterId) {
        return JSDELIVR_BASE + "data/chapters/chapter_" + pad(chapterId) + ".json";
    }

    private static String pad(int chapterId) {
        return String.format(Locale.US, "%02d", Integer.valueOf(chapterId));
    }
}
