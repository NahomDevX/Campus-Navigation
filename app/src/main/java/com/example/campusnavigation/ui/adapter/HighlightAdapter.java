package com.example.campusnavigation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusnavigation.R;

import java.util.ArrayList;
import java.util.List;

public class HighlightAdapter extends RecyclerView.Adapter<HighlightAdapter.HighlightViewHolder> {
    public static class HighlightItem {
        public final String title;
        public final String subtitle;

        public HighlightItem(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private final List<HighlightItem> items = new ArrayList<>();

    public void submitList(List<HighlightItem> highlights) {
        items.clear();
        if (highlights != null) {
            items.addAll(highlights);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HighlightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_highlight, parent, false);
        return new HighlightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HighlightViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HighlightViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView subtitleText;

        HighlightViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.highlightTitleText);
            subtitleText = itemView.findViewById(R.id.highlightSubtitleText);
        }

        void bind(HighlightItem item) {
            titleText.setText(item.title);
            subtitleText.setText(item.subtitle);
        }
    }
}
