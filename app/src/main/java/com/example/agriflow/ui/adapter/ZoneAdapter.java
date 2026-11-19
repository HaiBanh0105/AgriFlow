package com.example.agriflow.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agriflow.R;
import com.example.agriflow.data.model.Zone;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class ZoneAdapter extends RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder> {

    public interface OnZoneClickListener {
        void onZoneClick(Zone zone);
    }

    private List<Zone> zones = new ArrayList<>();
    private final OnZoneClickListener clickListener;

    public ZoneAdapter(OnZoneClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setZones(List<Zone> zones) {
        this.zones = zones != null ? zones : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ZoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_zone_card, parent, false);
        return new ZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZoneViewHolder holder, int position) {
        Zone zone = zones.get(position);
        holder.bind(zone, clickListener);
    }

    @Override
    public int getItemCount() {
        return zones.size();
    }

    static class ZoneViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvZoneName;
        private final TextView tvCropType;
        private final TextView tvDimension;
        private final TextView tvDescription;
        private final TextView tvHealthPercent;
        private final LinearProgressIndicator progressHealth;
        private final TextView tvCountHealthy;
        private final TextView tvCountWarning;
        private final TextView tvCountDiseased;
        private final TextView tvCountEmpty;
        private final MaterialButton btnOpenGrid;

        public ZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            tvZoneName = itemView.findViewById(R.id.tv_zone_name);
            tvCropType = itemView.findViewById(R.id.tv_crop_type);
            tvDimension = itemView.findViewById(R.id.tv_dimension);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvHealthPercent = itemView.findViewById(R.id.tv_health_percent);
            progressHealth = itemView.findViewById(R.id.progress_health);
            tvCountHealthy = itemView.findViewById(R.id.tv_count_healthy);
            tvCountWarning = itemView.findViewById(R.id.tv_count_warning);
            tvCountDiseased = itemView.findViewById(R.id.tv_count_diseased);
            tvCountEmpty = itemView.findViewById(R.id.tv_count_empty);
            btnOpenGrid = itemView.findViewById(R.id.btn_open_grid);
        }

        public void bind(Zone zone, OnZoneClickListener listener) {
            tvZoneName.setText(zone.getName());
            tvCropType.setText("🌱 " + zone.getDefaultCrop());
            tvDimension.setText(zone.getRows() + " × " + zone.getCols() + " ô");
            tvDescription.setText(zone.getDescription());

            int healthPercent = zone.getHealthPercentage();
            tvHealthPercent.setText(healthPercent + "% khỏe mạnh");
            progressHealth.setProgress(healthPercent);

            tvCountHealthy.setText("🟢 " + zone.getHealthyCount() + " Khỏe");
            tvCountWarning.setText("🟡 " + zone.getWarningCount() + " Chăm sóc");
            tvCountDiseased.setText("🔴 " + zone.getDiseasedCount() + " Sâu bệnh");
            tvCountEmpty.setText("⚪ " + zone.getEmptyCount() + " Trống");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onZoneClick(zone);
                }
            });

            btnOpenGrid.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onZoneClick(zone);
                }
            });
        }
    }
}
