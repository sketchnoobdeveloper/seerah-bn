package com.snd.assaheed.seerah;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Chapter reader with lazy block rendering, font size and progress. */
public final class SeerahReaderActivity extends Activity {
    public static final String EXTRA_CHAPTER_ID = "chapter_id";
    public static final String EXTRA_CHAPTER_TITLE = "chapter_title";
    public static final String EXTRA_CHAPTER_COUNT = "chapter_count";

    private static final String PREFS = "seerah_reader";
    private static final String KEY_FONT = "font_size";
    private static final float MIN_FONT = 14f;
    private static final float MAX_FONT = 30f;

    private RecyclerView list;
    private SeerahBlockAdapter adapter;
    private TextView title;
    private TextView progressText;
    private ProgressBar progress;
    private ProgressBar loading;
    private View errorBox;
    private TextView error;
    private Button retry;
    private Button previous;
    private Button next;

    private int chapterId = 1;
    private int chapterCount = 50;

    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            updateProgress();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seerah_reader);
        if (savedInstanceState != null) {
            chapterId = savedInstanceState.getInt(EXTRA_CHAPTER_ID, chapterId);
            chapterCount = savedInstanceState.getInt(EXTRA_CHAPTER_COUNT, chapterCount);
        } else {
            chapterId = getIntent().getIntExtra(EXTRA_CHAPTER_ID, 1);
            chapterCount = getIntent().getIntExtra(EXTRA_CHAPTER_COUNT, 50);
        }

        title = (TextView) findViewById(R.id.seerah_reader_title);
        progressText = (TextView) findViewById(R.id.seerah_reader_progress_text);
        progress = (ProgressBar) findViewById(R.id.seerah_reader_progress);
        loading = (ProgressBar) findViewById(R.id.seerah_reader_loading);
        errorBox = findViewById(R.id.seerah_reader_error_box);
        error = (TextView) findViewById(R.id.seerah_reader_error);
        retry = (Button) findViewById(R.id.seerah_reader_retry);
        previous = (Button) findViewById(R.id.seerah_reader_prev);
        next = (Button) findViewById(R.id.seerah_reader_next);
        list = (RecyclerView) findViewById(R.id.seerah_reader_list);

        adapter = new SeerahBlockAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.addOnScrollListener(scrollListener);
        adapter.setFontSize(fontSize());

        findViewById(R.id.seerah_reader_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        findViewById(R.id.seerah_reader_font_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFont(-1f);
            }
        });
        findViewById(R.id.seerah_reader_font_plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFont(1f);
            }
        });
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadChapter(chapterId);
            }
        });
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chapterId > 1) {
                    loadChapter(chapterId - 1);
                }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chapterId < chapterCount) {
                    loadChapter(chapterId + 1);
                }
            }
        });

        String extraTitle = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
        if (extraTitle != null && extraTitle.length() > 0) {
            title.setText(extraTitle);
        }
        loadChapter(chapterId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_CHAPTER_ID, chapterId);
        outState.putInt(EXTRA_CHAPTER_COUNT, chapterCount);
        super.onSaveInstanceState(outState);
    }

    private void loadChapter(final int id) {
        chapterId = id;
        loading.setVisibility(View.VISIBLE);
        errorBox.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        previous.setEnabled(false);
        next.setEnabled(false);
        new SeerahChapterLoader(this, new SeerahChapterLoader.Callback() {
            @Override
            public void onSuccess(SeerahChapter chapter) {
                loading.setVisibility(View.GONE);
                errorBox.setVisibility(View.GONE);
                list.setVisibility(View.VISIBLE);
                title.setText(chapter.title);
                adapter.setContent(chapter.content, chapter.highlights);
                list.scrollToPosition(0);
                previous.setEnabled(chapter.id > 1);
                next.setEnabled(chapter.id < chapterCount);
                updateProgress();
            }

            @Override
            public void onError(String message) {
                loading.setVisibility(View.GONE);
                error.setText(message);
                error.setVisibility(View.VISIBLE);
                retry.setVisibility(View.VISIBLE);
                errorBox.setVisibility(View.VISIBLE);
            }
        }).load(id);
    }

    private void changeFont(float delta) {
        float size = adapter.getFontSize() + delta;
        if (size < MIN_FONT) {
            size = MIN_FONT;
        }
        if (size > MAX_FONT) {
            size = MAX_FONT;
        }
        adapter.setFontSize(size);
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        preferences.edit().putFloat(KEY_FONT, size).apply();
    }

    private float fontSize() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        return preferences.getFloat(KEY_FONT, 18f);
    }

    private void updateProgress() {
        int range = list.computeVerticalScrollRange();
        int offset = list.computeVerticalScrollOffset();
        int percent = range <= 0 ? 0 : (int) (offset * 100L / range);
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        progress.setProgress(percent);
        progressText.setText(SeerahMessages.percent(percent));
    }
}
