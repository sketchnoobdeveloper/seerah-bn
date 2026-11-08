package com.snd.assaheed.seerah;

import java.util.ArrayList;
import java.util.List;

/** Parsed seerah_index.json. */
public final class SeerahIndex {
    public final String title;
    public final String author;
    public final int totalChapters;
    public final List<SeerahChapterInfo> chapters;

    public SeerahIndex(String title, String author, int totalChapters,
                       List<SeerahChapterInfo> chapters) {
        this.title = title;
        this.author = author;
        this.totalChapters = totalChapters;
        this.chapters = chapters == null ? new ArrayList<SeerahChapterInfo>() : chapters;
    }
}
