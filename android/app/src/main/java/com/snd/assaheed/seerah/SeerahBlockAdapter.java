package com.snd.assaheed.seerah;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the chapter's reading cues followed by headings and paragraphs.
 * Only visible rows are inflated, so a 50,000-character chapter stays smooth.
 */
public final class SeerahBlockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_PARAGRAPH = 0;
    private static final int TYPE_HEADING = 1;
    private static final int TYPE_HIGHLIGHTS = 2;

    private final List<SeerahBlock> blocks = new ArrayList<SeerahBlock>();
    private SeerahHighlights highlights = SeerahHighlights.EMPTY;
    private float fontSize = 18f;

    public void setContent(List<SeerahBlock> content, SeerahHighlights cues) {
        blocks.clear();
        if (content != null) {
            blocks.addAll(content);
        }
        highlights = cues == null ? SeerahHighlights.EMPTY : cues;
        notifyDataSetChanged();
    }

    public void setFontSize(float size) {
        fontSize = size;
        notifyDataSetChanged();
    }

    public float getFontSize() {
        return fontSize;
    }

    @Override
    public int getItemCount() {
        return blocks.size() + (hasHeader() ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (hasHeader() && position == 0) {
            return TYPE_HIGHLIGHTS;
        }
        return blocks.get(position - offset()).isHeading() ? TYPE_HEADING : TYPE_PARAGRAPH;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HIGHLIGHTS) {
            return new HighlightsHolder(inflater.inflate(R.layout.item_seerah_highlights, parent, false));
        }
        return new BlockHolder(inflater.inflate(R.layout.item_seerah_block, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HighlightsHolder) {
            bindHighlights((HighlightsHolder) holder);
            return;
        }
        bindBlock((BlockHolder) holder, blocks.get(position - offset()));
    }

    private boolean hasHeader() {
        return highlights.hasAny();
    }

    private int offset() {
        return hasHeader() ? 1 : 0;
    }

    private void bindHighlights(HighlightsHolder holder) {
        bindText(holder.subtitle, highlights.subtitle);
        bindRow(holder.lessonRow, holder.lesson, highlights.lesson);
        bindRow(holder.funRow, holder.fun, highlights.fun);
        bindRow(holder.practiceRow, holder.practice, highlights.practice);
    }

    private static void bindText(TextView view, String value) {
        if (value.length() == 0) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setText(value);
        }
    }

    private static void bindRow(View row, TextView text, String value) {
        if (value.length() == 0) {
            row.setVisibility(View.GONE);
        } else {
            row.setVisibility(View.VISIBLE);
            text.setText(value);
        }
    }

    private void bindBlock(BlockHolder holder, SeerahBlock block) {
        holder.text.setText(block.text);
        holder.text.setTextIsSelectable(true);
        if (block.isHeading()) {
            holder.text.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            holder.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize + 4f);
            holder.text.setTextColor(SeerahTheme.accent(holder.text.getContext()));
            holder.text.setLineSpacing(0f, 1.1f);
            holder.text.setPadding(0, dp(holder.text, 18), 0, dp(holder.text, 6));
        } else {
            holder.text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            holder.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            holder.text.setTextColor(SeerahTheme.primaryText(holder.text.getContext()));
            holder.text.setLineSpacing(dp(holder.text, 4), 1.15f);
            holder.text.setPadding(0, dp(holder.text, 4), 0, dp(holder.text, 10));
        }
    }

    private static int dp(TextView view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }

    static final class BlockHolder extends RecyclerView.ViewHolder {
        final TextView text;

        BlockHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.seerah_block_text);
        }
    }

    static final class HighlightsHolder extends RecyclerView.ViewHolder {
        final TextView subtitle;
        final View lessonRow;
        final TextView lesson;
        final View funRow;
        final TextView fun;
        final View practiceRow;
        final TextView practice;

        HighlightsHolder(View itemView) {
            super(itemView);
            subtitle = (TextView) itemView.findViewById(R.id.seerah_highlights_subtitle);
            lessonRow = itemView.findViewById(R.id.seerah_highlights_lesson_row);
            lesson = (TextView) itemView.findViewById(R.id.seerah_highlights_lesson);
            funRow = itemView.findViewById(R.id.seerah_highlights_fun_row);
            fun = (TextView) itemView.findViewById(R.id.seerah_highlights_fun);
            practiceRow = itemView.findViewById(R.id.seerah_highlights_practice_row);
            practice = (TextView) itemView.findViewById(R.id.seerah_highlights_practice);
        }
    }
}
