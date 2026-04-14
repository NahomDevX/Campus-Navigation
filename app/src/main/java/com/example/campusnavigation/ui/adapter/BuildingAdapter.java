package com.example.campusnavigation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusnavigation.R;
import com.example.campusnavigation.model.Building;

import java.util.ArrayList;
import java.util.List;

public class BuildingAdapter extends RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder> {
    public interface Listener {
        void onBuildingSelected(Building building);

        void onFavoriteToggled(Building building);
    }

    private final Listener listener;
    private final List<Building> items = new ArrayList<>();

    public BuildingAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Building> buildings) {
        items.clear();
        if (buildings != null) {
            items.addAll(buildings);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BuildingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_building, parent, false);
        return new BuildingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BuildingViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class BuildingViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView descriptionText;
        private final TextView typeText;

        BuildingViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.buildingNameText);
            descriptionText = itemView.findViewById(R.id.buildingDescriptionText);
            typeText = itemView.findViewById(R.id.buildingTypeText);
        }

        void bind(Building building) {
            nameText.setText(building.getName());
            descriptionText.setText(building.getDescription());
            typeText.setText(building.getType() + (building.isFavorite() ? " • Favorite" : ""));
            itemView.setOnClickListener(v -> listener.onBuildingSelected(building));
            itemView.setOnLongClickListener(v -> {
                listener.onFavoriteToggled(building);
                return true;
            });
        }
    }
}
