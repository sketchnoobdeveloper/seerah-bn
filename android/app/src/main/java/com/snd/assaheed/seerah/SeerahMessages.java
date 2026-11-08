package com.snd.assaheed.seerah;

import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;

/** Bangla error and number formatting shared by the screens. */
public final class SeerahMessages {
    private SeerahMessages() {
    }

    public static String friendly(Exception error) {
        if (error instanceof JSONException) {
            return "সীরাতের ডেটা পড়া যায়নি। ফাইলটি ঠিক আছে কিনা দেখুন।";
        }
        if (error instanceof IOException) {
            return "ইন্টারনেট সংযোগ নেই বা সার্ভারে পৌঁছানো যায়নি। সংরক্ষিত কপি থাকলে সেটি পড়ুন।";
        }
        return "কিছু একটা সমস্যা হয়েছে। আবার চেষ্টা করুন।";
    }

    public static String bytes(long value) {
        if (value < 1024L) {
            return bengaliDigits(String.valueOf(value)) + " বাইট";
        }
        double kb = value / 1024.0;
        if (kb < 1024.0) {
            return bengaliDigits(String.format(Locale.US, "%.1f", Double.valueOf(kb))) + " কেবি";
        }
        double mb = kb / 1024.0;
        return bengaliDigits(String.format(Locale.US, "%.1f", Double.valueOf(mb))) + " মেগাবাইট";
    }

    public static String percent(int value) {
        return bengaliDigits(String.valueOf(value)) + "%";
    }

    public static String bengaliDigits(String value) {
        char[] digits = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                builder.append(digits[c - '0']);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
