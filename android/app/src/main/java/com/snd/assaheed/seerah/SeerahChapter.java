package com.snd.assaheed.seerah;

import java.util.ArrayList;
import java.util.List;

/** One complete chapter returned by the online or offline loader. */
public final class SeerahChapter {
    public final int id;
    public final String title;
    public final String slug;
    public final String period;
    public final String summary;
    public final int charCount;
    public final List<SeerahBlock> content;

    public SeerahChapter(int id, String title, String slug, String period,
                         String summary, int charCount, List<SeerahBlock> content) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.period = period;
        this.summary = summary;
        this.charCount = charCount;
        this.content = content == null ? new ArrayList<SeerahBlock>() : content;
    }
}
