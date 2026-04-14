package com.example.campusnavigation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusnavigation.R;
import com.example.campusnavigation.model.CampusEvent;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private final List<CampusEvent> items = new ArrayList<>();

    public void submitList(List<CampusEvent> events) {
        items.clear();
        if (events != null) {
            items.addAll(events);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EventViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView locationText;
        private final TextView timeText;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.eventNameText);
            locationText = itemView.findViewById(R.id.eventLocationText);
            timeText = itemView.findViewById(R.id.eventTimeText);
        }

        void bind(CampusEvent event) {
            nameText.setText(event.getName());
            locationText.setText(event.getBuildingName());
            timeText.setText(DateFormat.getDateTimeInstance().format(new Date(event.getEventTimeMillis())));
        }
    }
}
