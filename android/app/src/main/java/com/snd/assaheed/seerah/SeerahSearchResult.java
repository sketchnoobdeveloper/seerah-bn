package com.snd.assaheed.seerah;

/** One search hit across the full book. */
public final class SeerahSearchResult {
    public final int chapterId;
    public final String chapterTitle;
    public final String snippet;
    public final String blockType;

    public SeerahSearchResult(int chapterId, String chapterTitle,
                              String snippet, String blockType) {
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
        this.snippet = snippet;
        this.blockType = blockType;
    }
}
