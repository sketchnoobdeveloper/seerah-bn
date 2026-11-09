package com.snd.assaheed.seerah;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/** Loads the table of contents from the saved copy or the GitHub raw CDN. */
public final class SeerahIndexLoader {
    public interface Callback {
        void onSuccess(SeerahIndex index);

        void onError(String message);
    }

    private final Context context;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());

    public SeerahIndexLoader(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    public void load() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SeerahLibraryManager library = new SeerahLibraryManager(context);
                    SeerahIndex loaded;
                    if (library.hasSavedCopy()) {
                        try {
                            loaded = SeerahJson.parseIndexFromFull(library.readSavedBook());
                        } catch (Exception savedCopyError) {
                            String raw = SeerahHttp.getWithFallback(
                                    SeerahUrls.INDEX, SeerahUrls.INDEX_JSDELIVR);
                            loaded = SeerahJson.parseIndex(raw);
                        }
                    } else {
                        String raw = SeerahHttp.getWithFallback(SeerahUrls.INDEX, SeerahUrls.INDEX_JSDELIVR);
                        loaded = SeerahJson.parseIndex(raw);
                    }
                    final SeerahIndex index = loaded;
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(index);
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
}
