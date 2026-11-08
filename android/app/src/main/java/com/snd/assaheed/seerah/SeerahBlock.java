package com.snd.assaheed.seerah;

/** A heading or paragraph inside a chapter. */
public final class SeerahBlock {
    public static final String TYPE_HEADING = "heading";
    public static final String TYPE_PARAGRAPH = "paragraph";

    public final String type;
    public final String text;

    public SeerahBlock(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public boolean isHeading() {
        return TYPE_HEADING.equals(type);
    }
}
