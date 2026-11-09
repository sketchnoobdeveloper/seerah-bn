package com.snd.assaheed.seerah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact premium chapter cards for the table of contents. */
public final class SeerahChapterAdapter extends RecyclerView.Adapter<SeerahChapterAdapter.Holder> {
    public interface OnChapterClickListener {
        void onChapterClick(SeerahChapterInfo chapter);
    }

    private final List<SeerahChapterInfo> items = new ArrayList<SeerahChapterInfo>();
    private final OnChapterClickListener listener;

    public SeerahChapterAdapter(OnChapterClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SeerahChapterInfo> chapters) {
        items.clear();
        if (chapters != null) {
            items.addAll(chapters);
        }
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seerah_chapter, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final SeerahChapterInfo chapter = items.get(position);
        SeerahHighlights highlights = chapter.highlights;

        holder.number.setText(SeerahMessages.bengaliDigits(
                String.format(Locale.US, "%02d", Integer.valueOf(chapter.id))));
        holder.title.setText(chapter.title);

        if (highlights.subtitle.length() == 0) {
            holder.subtitle.setVisibility(View.GONE);
        } else {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(highlights.subtitle);
        }
        if (chapter.period == null || chapter.period.length() == 0) {
            holder.period.setVisibility(View.GONE);
        } else {
            holder.period.setVisibility(View.VISIBLE);
            holder.period.setText(chapter.period);
        }

        bindRow(holder.lessonRow, holder.lesson, highlights.lesson);
        bindRow(holder.funRow, holder.fun, highlights.fun);
        bindRow(holder.practiceRow, holder.practice, highlights.practice);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onChapterClick(chapter);
            }
        });
    }

    private static void bindRow(View row, TextView text, String value) {
        if (value.length() == 0) {
            row.setVisibility(View.GONE);
        } else {
            row.setVisibility(View.VISIBLE);
            text.setText(value);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView title;
        final TextView subtitle;
        final TextView period;
        final View lessonRow;
        final TextView lesson;
        final View funRow;
        final TextView fun;
        final View practiceRow;
        final TextView practice;

        Holder(View itemView) {
            super(itemView);
            number = (TextView) itemView.findViewById(R.id.seerah_chapter_number);
            title = (TextView) itemView.findViewById(R.id.seerah_chapter_title);
            subtitle = (TextView) itemView.findViewById(R.id.seerah_chapter_subtitle);
            period = (TextView) itemView.findViewById(R.id.seerah_chapter_period);
            lessonRow = itemView.findViewById(R.id.seerah_row_lesson);
            lesson = (TextView) itemView.findViewById(R.id.seerah_chapter_lesson);
            funRow = itemView.findViewById(R.id.seerah_row_fun);
            fun = (TextView) itemView.findViewById(R.id.seerah_chapter_fun);
            practiceRow = itemView.findViewById(R.id.seerah_row_practice);
            practice = (TextView) itemView.findViewById(R.id.seerah_chapter_practice);
        }
    }
}
