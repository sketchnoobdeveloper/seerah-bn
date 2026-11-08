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

/** Renders headings and paragraphs lazily; long chapters never inflate all views. */
public final class SeerahBlockAdapter extends RecyclerView.Adapter<SeerahBlockAdapter.Holder> {
    private static final int TYPE_PARAGRAPH = 0;
    private static final int TYPE_HEADING = 1;

    private final List<SeerahBlock> blocks = new ArrayList<SeerahBlock>();
    private float fontSize = 18f;

    public void setBlocks(List<SeerahBlock> content) {
        blocks.clear();
        if (content != null) {
            blocks.addAll(content);
        }
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
    public int getItemViewType(int position) {
        return blocks.get(position).isHeading() ? TYPE_HEADING : TYPE_PARAGRAPH;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seerah_block, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        SeerahBlock block = blocks.get(position);
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

    @Override
    public int getItemCount() {
        return blocks.size();
    }

    private static int dp(TextView view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView text;

        Holder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.seerah_block_text);
        }
    }
}
