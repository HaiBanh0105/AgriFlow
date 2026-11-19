package com.example.agriflow.ui.adapter;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agriflow.R;
import com.example.agriflow.data.model.Zone;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class ZoneAdapter extends RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder> {

    public interface OnZoneActionListener {
        void onZoneClick(Zone zone);
        void onEditZone(Zone zone);
        void onDeleteZone(Zone zone);
    }

    private List<Zone> zones = new ArrayList<>();
    private final OnZoneActionListener actionListener;

    public ZoneAdapter(OnZoneActionListener actionListener) {
        this.actionListener = actionListener;
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
        holder.bind(zone, actionListener);
    }

    @Override
    public int getItemCount() {
        return zones.size();
    }

    static class ZoneViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvZoneName;
        private final TextView tvCropType;
        private final TextView tvDimension;
        private final ImageButton btnZoneOptions;
        private final TextView tvDescription;
        private final TextView tvHarvestSummary;
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
            btnZoneOptions = itemView.findViewById(R.id.btn_zone_options);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvHarvestSummary = itemView.findViewById(R.id.tv_harvest_summary);
            tvHealthPercent = itemView.findViewById(R.id.tv_health_percent);
            progressHealth = itemView.findViewById(R.id.progress_health);
            tvCountHealthy = itemView.findViewById(R.id.tv_count_healthy);
            tvCountWarning = itemView.findViewById(R.id.tv_count_warning);
            tvCountDiseased = itemView.findViewById(R.id.tv_count_diseased);
            tvCountEmpty = itemView.findViewById(R.id.tv_count_empty);
            btnOpenGrid = itemView.findViewById(R.id.btn_open_grid);
        }

        public void bind(Zone zone, OnZoneActionListener listener) {
            tvZoneName.setText(zone.getName());
            tvCropType.setText("🌱 " + zone.getDefaultCrop());
            tvDimension.setText(zone.getRows() + " × " + zone.getCols() + " ô");
            tvDescription.setText(zone.getDescription());

            // Harvest Summary badge
            int dueCount = zone.getDueForHarvestCount();
            int recentCount = zone.getHarvestedRecentlyCount(15);
            if (dueCount > 0) {
                tvHarvestSummary.setVisibility(View.VISIBLE);
                tvHarvestSummary.setText("⏳ " + dueCount + " cây đến kỳ thu hoạch • Chu kỳ " + zone.getHarvestCycleDays() + " ngày");
                tvHarvestSummary.setTextColor(0xFFC62828); // Red
            } else if (recentCount > 0) {
                tvHarvestSummary.setVisibility(View.VISIBLE);
                tvHarvestSummary.setText("🕒 " + recentCount + " cây vừa thu hoạch gần đây (< 15 ngày)");
                tvHarvestSummary.setTextColor(0xFF0277BD); // Blue
            } else {
                tvHarvestSummary.setVisibility(View.GONE);
            }

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

            btnZoneOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add(0, 1, 0, "✏️ Chỉnh sửa thông tin khu");
                popup.getMenu().add(0, 2, 1, "🗑️ Xóa khu vườn này");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        if (listener != null) listener.onEditZone(zone);
                        return true;
                    } else if (item.getItemId() == 2) {
                        if (listener != null) listener.onDeleteZone(zone);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
