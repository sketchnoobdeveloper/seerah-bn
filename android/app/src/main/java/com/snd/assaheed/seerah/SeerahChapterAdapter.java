package com.snd.assaheed.seerah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chapter list adapter for the index screen. */
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
        holder.number.setText(SeerahMessages.bengaliDigits(
                String.format(Locale.US, "%02d", Integer.valueOf(chapter.id))));
        holder.title.setText(chapter.title);
        if (chapter.period == null || chapter.period.length() == 0) {
            holder.period.setVisibility(View.GONE);
        } else {
            holder.period.setVisibility(View.VISIBLE);
            holder.period.setText(chapter.period);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onChapterClick(chapter);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView title;
        final TextView period;

        Holder(View itemView) {
            super(itemView);
            number = (TextView) itemView.findViewById(R.id.seerah_chapter_number);
            title = (TextView) itemView.findViewById(R.id.seerah_chapter_title);
            period = (TextView) itemView.findViewById(R.id.seerah_chapter_period);
        }
    }
}
