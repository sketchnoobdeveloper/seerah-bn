package com.snd.assaheed.seerah;

import android.content.Context;
import android.util.TypedValue;

/** Resolves platform theme colours so both day and night layouts stay readable. */
public final class SeerahTheme {
    private SeerahTheme() {
    }

    public static int color(Context context, int attribute) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) {
                return context.getResources().getColor(value.resourceId);
            }
            return value.data;
        }
        return 0xFF000000;
    }

    public static int primaryText(Context context) {
        return color(context, android.R.attr.textColorPrimary);
    }

    public static int secondaryText(Context context) {
        return color(context, android.R.attr.textColorSecondary);
    }

    public static int accent(Context context) {
        return color(context, android.R.attr.colorAccent);
    }
}
