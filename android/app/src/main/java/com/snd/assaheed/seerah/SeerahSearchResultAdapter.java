package com.snd.assaheed.seerah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Search result list. */
public final class SeerahSearchResultAdapter
        extends RecyclerView.Adapter<SeerahSearchResultAdapter.Holder> {
    public interface OnResultClickListener {
        void onResultClick(SeerahSearchResult result);
    }

    private final List<SeerahSearchResult> items = new ArrayList<SeerahSearchResult>();
    private final OnResultClickListener listener;

    public SeerahSearchResultAdapter(OnResultClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<SeerahSearchResult> results) {
        items.clear();
        if (results != null) {
            items.addAll(results);
        }
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seerah_search_result, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final SeerahSearchResult result = items.get(position);
        holder.title.setText(result.chapterTitle);
        holder.snippet.setText(result.snippet);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onResultClick(result);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView snippet;

        Holder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.seerah_search_title);
            snippet = (TextView) itemView.findViewById(R.id.seerah_search_snippet);
        }
    }
}
