package com.snd.assaheed.seerah;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** org.json parsing for the seerah-bn schema. */
public final class SeerahJson {
    private SeerahJson() {
    }

    public static SeerahIndex parseIndex(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        JSONObject meta = root.optJSONObject("meta");
        JSONArray chapters = root.optJSONArray("chapters");
        if (chapters == null) {
            throw new JSONException("chapters array is missing");
        }
        List<SeerahChapterInfo> items = new ArrayList<SeerahChapterInfo>();
        for (int i = 0; i < chapters.length(); i++) {
            JSONObject item = chapters.optJSONObject(i);
            if (item != null) {
                items.add(parseChapterInfo(item));
            }
        }
        String title = meta == null ? "আর-রাহীকুল মাখতুম" : meta.optString("title", "আর-রাহীকুল মাখতুম");
        String author = meta == null ? "" : meta.optString("author", "");
        int total = meta == null ? items.size() : meta.optInt("total_chapters", items.size());
        return new SeerahIndex(title, author, total, items);
    }

    public static SeerahIndex parseIndexFromFull(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        JSONObject meta = root.optJSONObject("meta");
        JSONArray chapters = root.optJSONArray("chapters");
        if (chapters == null) {
            throw new JSONException("chapters array is missing");
        }
        List<SeerahChapterInfo> items = new ArrayList<SeerahChapterInfo>();
        for (int i = 0; i < chapters.length(); i++) {
            JSONObject item = chapters.optJSONObject(i);
            if (item != null) {
                items.add(parseChapterInfo(item));
            }
        }
        String title = meta == null ? "আর-রাহীকুল মাখতুম" : meta.optString("title", "আর-রাহীকুল মাখতুম");
        String author = meta == null ? "" : meta.optString("author", "");
        int total = meta == null ? items.size() : meta.optInt("total_chapters", items.size());
        return new SeerahIndex(title, author, total, items);
    }

    public static SeerahChapter parseChapter(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        JSONObject chapter = root.optJSONObject("chapter");
        if (chapter == null) {
            chapter = root;
        }
        return parseChapterObject(chapter);
    }

    public static List<SeerahChapter> parseAllChapters(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        JSONArray chapters = root.optJSONArray("chapters");
        if (chapters == null) {
            throw new JSONException("chapters array is missing");
        }
        List<SeerahChapter> result = new ArrayList<SeerahChapter>();
        for (int i = 0; i < chapters.length(); i++) {
            JSONObject item = chapters.optJSONObject(i);
            if (item != null) {
                result.add(parseChapterObject(item));
            }
        }
        return result;
    }

    public static SeerahChapter parseChapterFromFull(String raw, int chapterId) throws JSONException {
        JSONObject root = new JSONObject(raw);
        JSONArray chapters = root.optJSONArray("chapters");
        if (chapters == null) {
            throw new JSONException("chapters array is missing");
        }
        for (int i = 0; i < chapters.length(); i++) {
            JSONObject item = chapters.optJSONObject(i);
            if (item != null && item.optInt("id", -1) == chapterId) {
                return parseChapterObject(item);
            }
        }
        throw new JSONException("chapter not found: " + chapterId);
    }

    public static SeerahChapterInfo parseChapterInfo(JSONObject item) {
        int id = item.optInt("id", 0);
        String title = item.optString("title", "অধ্যায় " + id);
        String slug = item.optString("slug", String.format(Locale.US, "chapter-%02d", Integer.valueOf(id)));
        String period = item.optString("period", "");
        String summary = item.optString("summary", "");
        SeerahHighlights highlights = parseHighlights(item);
        int charCount = item.optInt("char_count", 0);
        String file = item.optString("file",
                String.format(Locale.US, "data/chapters/chapter_%02d.json", Integer.valueOf(id)));
        return new SeerahChapterInfo(id, title, slug, period, summary, highlights, charCount, file);
    }

    private static SeerahChapter parseChapterObject(JSONObject chapter) {
        int id = chapter.optInt("id", 0);
        String title = chapter.optString("title", "অধ্যায় " + id);
        String slug = chapter.optString("slug", String.format(Locale.US, "chapter-%02d", Integer.valueOf(id)));
        String period = chapter.optString("period", "");
        String summary = chapter.optString("summary", "");
        SeerahHighlights highlights = parseHighlights(chapter);
        int charCount = chapter.optInt("char_count", 0);
        List<SeerahBlock> blocks = parseContent(chapter.optJSONArray("content"));
        return new SeerahChapter(id, title, slug, period, summary, highlights, charCount, blocks);
    }

    private static SeerahHighlights parseHighlights(JSONObject item) {
        JSONObject highlights = item.optJSONObject("highlights");
        if (highlights == null) {
            return SeerahHighlights.EMPTY;
        }
        return new SeerahHighlights(
                highlights.optString("subtitle", ""),
                highlights.optString("lesson", ""),
                highlights.optString("fun", ""),
                highlights.optString("practice", ""));
    }

    private static List<SeerahBlock> parseContent(JSONArray content) {
        List<SeerahBlock> blocks = new ArrayList<SeerahBlock>();
        if (content == null) {
            return blocks;
        }
        for (int i = 0; i < content.length(); i++) {
            JSONObject item = content.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String type = item.optString("type", SeerahBlock.TYPE_PARAGRAPH);
            String text = item.optString("text", "");
            if (text.length() == 0) {
                continue;
            }
            if (!SeerahBlock.TYPE_HEADING.equals(type)) {
                type = SeerahBlock.TYPE_PARAGRAPH;
            }
            blocks.add(new SeerahBlock(type, text));
        }
        return blocks;
    }
}
