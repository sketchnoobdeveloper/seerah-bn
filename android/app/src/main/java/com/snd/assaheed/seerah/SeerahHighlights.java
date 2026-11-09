package com.snd.assaheed.seerah;

/** Short reading cues shown on the chapter card and at the top of the reader. */
public final class SeerahHighlights {
    public static final SeerahHighlights EMPTY = new SeerahHighlights("", "", "", "");

    public final String subtitle;
    public final String lesson;
    public final String fun;
    public final String practice;

    public SeerahHighlights(String subtitle, String lesson, String fun, String practice) {
        this.subtitle = safe(subtitle);
        this.lesson = safe(lesson);
        this.fun = safe(fun);
        this.practice = safe(practice);
    }

    public boolean hasAny() {
        return subtitle.length() > 0 || lesson.length() > 0
                || fun.length() > 0 || practice.length() > 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
