package com.snd.assaheed.seerah;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Searches every chapter body. The full book is loaded once and cached in
 * memory for ten minutes; the search itself runs on a background thread.
 */
public final class SeerahSearchManager {
    public interface Callback {
        void onSuccess(List<SeerahSearchResult> results);

        void onError(String message);
    }

    private static final int MAX_RESULTS = 200;
    private static final int MAX_PER_CHAPTER = 5;
    private static final long CACHE_MILLIS = 10L * 60L * 1000L;

    private static String cachedRaw;
    private static long cachedAt;

    private final Context context;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());

    public SeerahSearchManager(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    public void search(final String query) {
        final String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.length() < 2) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(new ArrayList<SeerahSearchResult>());
                }
            });
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String raw = loadFull();
                    List<SeerahChapter> chapters = SeerahJson.parseAllChapters(raw);
                    final List<SeerahSearchResult> results = searchChapters(chapters, needle);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(results);
                        }
                    });
                } catch (final Exception error) {
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(SeerahMessages.friendly(error));
                        }
                    });
                }
            }
        }).start();
    }

    private String loadFull() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedRaw != null && now - cachedAt < CACHE_MILLIS) {
            return cachedRaw;
        }
        OfflineSeerahManager offline = new OfflineSeerahManager(context);
        String raw;
        if (offline.isAvailableOffline()) {
            try {
                raw = offline.readFull();
            } catch (Exception offlineError) {
                raw = SeerahHttp.getWithFallback(SeerahUrls.FULL, SeerahUrls.FULL_JSDELIVR);
            }
        } else {
            raw = SeerahHttp.getWithFallback(SeerahUrls.FULL, SeerahUrls.FULL_JSDELIVR);
        }
        cachedRaw = raw;
        cachedAt = now;
        return raw;
    }

    private List<SeerahSearchResult> searchChapters(List<SeerahChapter> chapters, String needle) {
        List<SeerahSearchResult> results = new ArrayList<SeerahSearchResult>();
        for (int i = 0; i < chapters.size() && results.size() < MAX_RESULTS; i++) {
            SeerahChapter chapter = chapters.get(i);
            int matches = 0;
            if (chapter.title.toLowerCase(Locale.ROOT).contains(needle)) {
                String snippet = chapter.summary.length() > 0
                        ? chapter.summary
                        : (chapter.content.isEmpty() ? chapter.title : chapter.content.get(0).text);
                results.add(new SeerahSearchResult(chapter.id, chapter.title,
                        snippet, SeerahBlock.TYPE_HEADING));
                matches++;
            }
            for (int j = 0; j < chapter.content.size() && matches < MAX_PER_CHAPTER; j++) {
                SeerahBlock block = chapter.content.get(j);
                String haystack = block.text.toLowerCase(Locale.ROOT);
                int index = haystack.indexOf(needle);
                if (index < 0) {
                    continue;
                }
                results.add(new SeerahSearchResult(chapter.id, chapter.title,
                        snippet(block.text, index, needle.length()), block.type));
                matches++;
                if (results.size() >= MAX_RESULTS) {
                    break;
                }
            }
        }
        return results;
    }

    private static String snippet(String text, int index, int length) {
        int start = index - 60;
        if (start < 0) {
            start = 0;
        }
        int end = index + length + 100;
        if (end > text.length()) {
            end = text.length();
        }
        String value = text.substring(start, end).replace('\n', ' ').trim();
        if (start > 0) {
            value = "…" + value;
        }
        if (end < text.length()) {
            value = value + "…";
        }
        return value;
    }
}
