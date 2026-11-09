package com.snd.assaheed.seerah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Chapter library: online by default, with an optional saved copy. */
public final class SeerahIndexActivity extends Activity {
    private RecyclerView list;
    private SeerahChapterAdapter adapter;
    private ProgressBar loading;
    private View errorBox;
    private TextView error;
    private Button retry;
    private TextView libraryChip;
    private View promo;
    private TextView promoText;
    private TextView promoHint;
    private Button save;
    private ProgressBar downloadProgress;
    private TextView downloadPercent;

    private SeerahLibraryManager library;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean polling;
    private boolean finalizing;
    private int chapterCount = 50;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            long id = library.getDownloadId();
            if (id < 0L) {
                stopPolling();
                showPromoReady();
                return;
            }
            SeerahLibraryManager.DownloadProgress progress = library.queryProgress(id);
            if (progress.isSuccessful()) {
                downloadPercent.setText(R.string.seerah_verifying);
                finishDownload(id);
                return;
            }
            if (progress.isFailed() || progress.isPaused()) {
                stopPolling();
                showPromoReady();
                Toast.makeText(SeerahIndexActivity.this,
                        R.string.seerah_download_failed, Toast.LENGTH_LONG).show();
                return;
            }
            downloadProgress.setVisibility(View.VISIBLE);
            downloadPercent.setVisibility(View.VISIBLE);
            int percent = progress.percent();
            downloadProgress.setProgress(percent);
            downloadPercent.setText(SeerahMessages.percent(percent));
            handler.postDelayed(this, 500L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seerah_index);
        library = new SeerahLibraryManager(this);

        list = (RecyclerView) findViewById(R.id.seerah_index_list);
        loading = (ProgressBar) findViewById(R.id.seerah_index_loading);
        errorBox = findViewById(R.id.seerah_index_error_box);
        error = (TextView) findViewById(R.id.seerah_index_error);
        retry = (Button) findViewById(R.id.seerah_index_retry);
        libraryChip = (TextView) findViewById(R.id.seerah_library_chip);
        promo = findViewById(R.id.seerah_promo);
        promoText = (TextView) findViewById(R.id.seerah_promo_text);
        promoHint = (TextView) findViewById(R.id.seerah_promo_hint);
        save = (Button) findViewById(R.id.seerah_save);
        downloadProgress = (ProgressBar) findViewById(R.id.seerah_download_progress);
        downloadPercent = (TextView) findViewById(R.id.seerah_download_percent);

        adapter = new SeerahChapterAdapter(new SeerahChapterAdapter.OnChapterClickListener() {
            @Override
            public void onChapterClick(SeerahChapterInfo chapter) {
                openReader(chapter);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.seerah_index_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SeerahIndexActivity.this, SeerahSearchActivity.class));
            }
        });
        findViewById(R.id.seerah_index_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenu(view);
            }
        });
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadIndex();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDownload();
            }
        });

        loadIndex();
        refreshLibraryUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLibraryUi();
        long id = library.getDownloadId();
        if (id >= 0L) {
            SeerahLibraryManager.DownloadProgress progress = library.queryProgress(id);
            if (progress.isSuccessful()) {
                finishDownload(id);
            } else if (progress.isRunning() || progress.isPending()) {
                startPolling();
            } else if (progress.isFailed()) {
                library.clearDownloadId();
                refreshLibraryUi();
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void loadIndex() {
        loading.setVisibility(View.VISIBLE);
        errorBox.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        new SeerahIndexLoader(this, new SeerahIndexLoader.Callback() {
            @Override
            public void onSuccess(SeerahIndex index) {
                loading.setVisibility(View.GONE);
                errorBox.setVisibility(View.GONE);
                list.setVisibility(View.VISIBLE);
                chapterCount = index.chapters.size();
                adapter.setItems(index.chapters);
            }

            @Override
            public void onError(String message) {
                loading.setVisibility(View.GONE);
                error.setText(message);
                error.setVisibility(View.VISIBLE);
                retry.setVisibility(View.VISIBLE);
                errorBox.setVisibility(View.VISIBLE);
            }
        }).load();
    }

    private void openReader(SeerahChapterInfo chapter) {
        Intent intent = new Intent(this, SeerahReaderActivity.class);
        intent.putExtra(SeerahReaderActivity.EXTRA_CHAPTER_ID, chapter.id);
        intent.putExtra(SeerahReaderActivity.EXTRA_CHAPTER_TITLE, chapter.title);
        intent.putExtra(SeerahReaderActivity.EXTRA_CHAPTER_COUNT, chapterCount);
        startActivity(intent);
    }

    /** The promo disappears completely once a copy is saved. */
    private void refreshLibraryUi() {
        if (library.hasSavedCopy()) {
            libraryChip.setVisibility(View.VISIBLE);
            libraryChip.setText(getString(R.string.seerah_saved_chip_size,
                    getString(R.string.seerah_saved_chip),
                    SeerahMessages.bytes(library.getSavedSize())));
            promo.setVisibility(View.GONE);
            save.setEnabled(true);
        } else {
            libraryChip.setVisibility(View.GONE);
            if (!polling && !finalizing) {
                showPromoReady();
            }
        }
    }

    private void showPromoReady() {
        if (library.hasSavedCopy()) {
            promo.setVisibility(View.GONE);
            return;
        }
        promo.setVisibility(View.VISIBLE);
        promoText.setText(R.string.seerah_promo_text);
        promoHint.setVisibility(View.VISIBLE);
        save.setEnabled(true);
        save.setText(R.string.seerah_save);
        downloadProgress.setVisibility(View.GONE);
        downloadPercent.setVisibility(View.GONE);
    }

    private void startDownload() {
        if (library.isDownloadRunning()) {
            startPolling();
            return;
        }
        try {
            library.enqueueDownload();
            promo.setVisibility(View.VISIBLE);
            promoText.setText(R.string.seerah_downloading);
            promoHint.setVisibility(View.GONE);
            save.setEnabled(false);
            downloadProgress.setProgress(0);
            downloadProgress.setVisibility(View.VISIBLE);
            downloadPercent.setText(SeerahMessages.percent(0));
            downloadPercent.setVisibility(View.VISIBLE);
            startPolling();
        } catch (Exception error) {
            Toast.makeText(this, SeerahMessages.friendly(error), Toast.LENGTH_LONG).show();
        }
    }

    private void startPolling() {
        if (polling) {
            return;
        }
        polling = true;
        promo.setVisibility(View.VISIBLE);
        promoText.setText(R.string.seerah_downloading);
        promoHint.setVisibility(View.GONE);
        save.setEnabled(false);
        handler.post(pollRunnable);
    }

    private void stopPolling() {
        polling = false;
        handler.removeCallbacks(pollRunnable);
    }

    private void finishDownload(final long id) {
        if (finalizing) {
            return;
        }
        finalizing = true;
        stopPolling();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = library.finalizeDownload(id);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        finalizing = false;
                        if (ok) {
                            refreshLibraryUi();
                            Toast.makeText(SeerahIndexActivity.this,
                                    R.string.seerah_saved_toast, Toast.LENGTH_LONG).show();
                        } else {
                            showPromoReady();
                            Toast.makeText(SeerahIndexActivity.this,
                                    R.string.seerah_saved_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, R.string.seerah_about);
        if (library.hasSavedCopy()) {
            popup.getMenu().add(0, 2, 1, R.string.seerah_delete_saved);
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    showAbout();
                    return true;
                }
                if (item.getItemId() == 2) {
                    confirmDelete();
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.seerah_about)
                .setMessage(R.string.seerah_about_message)
                .setPositiveButton(R.string.seerah_cancel, null)
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.seerah_delete_title)
                .setMessage(R.string.seerah_delete_message)
                .setPositiveButton(R.string.seerah_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        library.deleteSavedCopy();
                        refreshLibraryUi();
                    }
                })
                .setNegativeButton(R.string.seerah_cancel, null)
                .show();
    }
}
