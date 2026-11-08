package com.snd.assaheed.seerah;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Keyword search across every chapter body. */
public final class SeerahSearchActivity extends Activity {
    private EditText input;
    private RecyclerView list;
    private SeerahSearchResultAdapter adapter;
    private ProgressBar progress;
    private TextView empty;
    private TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seerah_search);

        input = (EditText) findViewById(R.id.seerah_search_input);
        list = (RecyclerView) findViewById(R.id.seerah_search_list);
        progress = (ProgressBar) findViewById(R.id.seerah_search_progress);
        empty = (TextView) findViewById(R.id.seerah_search_empty);
        error = (TextView) findViewById(R.id.seerah_search_error);

        adapter = new SeerahSearchResultAdapter(new SeerahSearchResultAdapter.OnResultClickListener() {
            @Override
            public void onResultClick(SeerahSearchResult result) {
                Intent intent = new Intent(SeerahSearchActivity.this, SeerahReaderActivity.class);
                intent.putExtra(SeerahReaderActivity.EXTRA_CHAPTER_ID, result.chapterId);
                intent.putExtra(SeerahReaderActivity.EXTRA_CHAPTER_TITLE, result.chapterTitle);
                startActivity(intent);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.seerah_search_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        findViewById(R.id.seerah_search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runSearch();
            }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    runSearch();
                    return true;
                }
                return false;
            }
        });
    }

    private void runSearch() {
        String query = input.getText().toString().trim();
        InputMethodManager keyboard =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
        if (query.length() < 2) {
            adapter.setItems(null);
            list.setVisibility(View.GONE);
            empty.setText("কমপক্ষে দুই অক্ষরের শব্দ লিখুন।");
            empty.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        new SeerahSearchManager(this, new SeerahSearchManager.Callback() {
            @Override
            public void onSuccess(List<SeerahSearchResult> results) {
                progress.setVisibility(View.GONE);
                adapter.setItems(results);
                if (results.isEmpty()) {
                    empty.setText("কোনো ফল পাওয়া যায়নি।");
                    empty.setVisibility(View.VISIBLE);
                    list.setVisibility(View.GONE);
                } else {
                    empty.setVisibility(View.GONE);
                    list.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                error.setText(message);
                error.setVisibility(View.VISIBLE);
                list.setVisibility(View.GONE);
            }
        }).search(query);
    }
}
