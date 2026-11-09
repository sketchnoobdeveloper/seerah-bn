package com.snd.assaheed.seerah;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Loads one chapter. Saved library copy wins; otherwise the small per-chapter
 * file is fetched, with the full book as a second online fallback.
 */
public final class SeerahChapterLoader {
    public interface Callback {
        void onSuccess(SeerahChapter chapter);

        void onError(String message);
    }

    private final Context context;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());

    public SeerahChapterLoader(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    public void load(final int chapterId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SeerahLibraryManager library = new SeerahLibraryManager(context);
                    SeerahChapter loaded;
                    if (library.hasSavedCopy()) {
                        try {
                            loaded = library.readChapter(chapterId);
                        } catch (Exception savedCopyError) {
                            loaded = loadOnline(chapterId);
                        }
                    } else {
                        loaded = loadOnline(chapterId);
                    }
                    final SeerahChapter chapter = loaded;
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(chapter);
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

    private SeerahChapter loadOnline(int chapterId) throws Exception {
        try {
            String raw = SeerahHttp.getWithFallback(
                    SeerahUrls.chapter(chapterId), SeerahUrls.chapterJsdelivr(chapterId));
            return SeerahJson.parseChapter(raw);
        } catch (Exception chapterError) {
            String raw = SeerahHttp.getWithFallback(SeerahUrls.FULL, SeerahUrls.FULL_JSDELIVR);
            return SeerahJson.parseChapterFromFull(raw, chapterId);
        }
    }
}
