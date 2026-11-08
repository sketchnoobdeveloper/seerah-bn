package com.snd.assaheed.seerah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Table of contents, online loading, optional download and offline status. */
public final class SeerahIndexActivity extends Activity {
    private RecyclerView list;
    private SeerahChapterAdapter adapter;
    private ProgressBar loading;
    private View errorBox;
    private TextView error;
    private Button retry;
    private TextView offlineStatus;
    private TextView offlineSize;
    private Button download;
    private Button delete;
    private ProgressBar downloadProgress;
    private TextView downloadPercent;

    private OfflineSeerahManager offline;
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
            long id = offline.getDownloadId();
            if (id < 0L) {
                stopPolling();
                return;
            }
            OfflineSeerahManager.DownloadProgress progress = offline.queryProgress(id);
            if (progress.isSuccessful()) {
                downloadPercent.setText("যাচাই করা হচ্ছে…");
                finishDownload(id);
                return;
            }
            if (progress.isFailed() || progress.isPaused()) {
                stopPolling();
                download.setEnabled(true);
                downloadProgress.setVisibility(View.GONE);
                downloadPercent.setVisibility(View.GONE);
                Toast.makeText(SeerahIndexActivity.this,
                        "ডাউনলোড সম্পূর্ণ হয়নি। আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show();
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
        offline = new OfflineSeerahManager(this);

        list = (RecyclerView) findViewById(R.id.seerah_index_list);
        loading = (ProgressBar) findViewById(R.id.seerah_index_loading);
        errorBox = findViewById(R.id.seerah_index_error_box);
        error = (TextView) findViewById(R.id.seerah_index_error);
        retry = (Button) findViewById(R.id.seerah_index_retry);
        offlineStatus = (TextView) findViewById(R.id.seerah_offline_status);
        offlineSize = (TextView) findViewById(R.id.seerah_offline_size);
        download = (Button) findViewById(R.id.seerah_download);
        delete = (Button) findViewById(R.id.seerah_delete);
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
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadIndex();
            }
        });
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDownload();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDelete();
            }
        });

        loadIndex();
        refreshOfflineUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshOfflineUi();
        long id = offline.getDownloadId();
        if (id >= 0L) {
            OfflineSeerahManager.DownloadProgress progress = offline.queryProgress(id);
            if (progress.isSuccessful()) {
                finishDownload(id);
            } else if (progress.isRunning() || progress.isPending()) {
                startPolling();
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

    private void refreshOfflineUi() {
        if (offline.isAvailableOffline()) {
            offlineStatus.setText("অফলাইন কপি প্রস্তুত");
            offlineSize.setVisibility(View.VISIBLE);
            offlineSize.setText("সংরক্ষিত আকার: " + SeerahMessages.bytes(offline.getOfflineSize()));
            download.setText("আবার ডাউনলোড");
            delete.setVisibility(View.VISIBLE);
        } else {
            offlineStatus.setText("অনলাইন মোড");
            offlineSize.setVisibility(View.GONE);
            download.setText("অফলাইনে ডাউনলোড");
            delete.setVisibility(View.GONE);
        }
        download.setEnabled(true);
    }

    private void startDownload() {
        if (polling || offline.isDownloadRunning()) {
            startPolling();
            return;
        }
        try {
            offline.enqueueDownload();
            download.setEnabled(false);
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
        download.setEnabled(false);
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
                final boolean ok = offline.finalizeDownload(id);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        finalizing = false;
                        downloadProgress.setVisibility(View.GONE);
                        downloadPercent.setVisibility(View.GONE);
                        download.setEnabled(true);
                        refreshOfflineUi();
                        Toast.makeText(SeerahIndexActivity.this,
                                ok ? "সীরাত অফলাইনে সংরক্ষিত হয়েছে।"
                                        : "সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("অফলাইন কপি মুছবেন?")
                .setMessage("সংরক্ষিত সীরাত ফাইলটি ফোন থেকে মুছে যাবে।")
                .setPositiveButton("মুছুন", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        offline.deleteOffline();
                        refreshOfflineUi();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }
}
