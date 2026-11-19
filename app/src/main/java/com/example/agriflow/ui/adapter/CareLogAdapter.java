package com.example.agriflow.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agriflow.R;
import com.example.agriflow.data.model.CareLog;

import java.util.ArrayList;
import java.util.List;

public class CareLogAdapter extends RecyclerView.Adapter<CareLogAdapter.CareLogViewHolder> {

    private List<CareLog> logs = new ArrayList<>();

    public void setLogs(List<CareLog> logs) {
        this.logs = logs != null ? logs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CareLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_care_log, parent, false);
        return new CareLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CareLogViewHolder holder, int position) {
        CareLog log = logs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class CareLogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEmoji;
        private final TextView tvName;
        private final TextView tvTime;
        private final TextView tvNote;

        public CareLogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_action_emoji);
            tvName = itemView.findViewById(R.id.tv_action_name);
            tvTime = itemView.findViewById(R.id.tv_log_time);
            tvNote = itemView.findViewById(R.id.tv_log_note);
        }

        public void bind(CareLog log) {
            if (log.getActionType() != null) {
                tvEmoji.setText(log.getActionType().getIconEmoji());
                tvName.setText(log.getActionType().getDisplayName());
            } else {
                tvEmoji.setText("📝");
                tvName.setText("Hoạt động");
            }
            tvTime.setText(log.getRelativeTime());
            tvNote.setText(log.getNote() != null ? log.getNote() : "");
        }
    }
}
