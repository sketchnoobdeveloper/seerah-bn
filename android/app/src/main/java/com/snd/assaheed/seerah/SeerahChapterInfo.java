package com.snd.assaheed.seerah;

/** One entry in the fast-loading table of contents. */
public final class SeerahChapterInfo {
    public final int id;
    public final String title;
    public final String slug;
    public final String period;
    public final String summary;
    public final SeerahHighlights highlights;
    public final int charCount;
    public final String file;

    public SeerahChapterInfo(int id, String title, String slug, String period,
                             String summary, SeerahHighlights highlights,
                             int charCount, String file) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.period = period;
        this.summary = summary;
        this.highlights = highlights == null ? SeerahHighlights.EMPTY : highlights;
        this.charCount = charCount;
        this.file = file;
    }
}
