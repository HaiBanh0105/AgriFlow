package com.example.agriflow.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agriflow.R;
import com.example.agriflow.data.model.CareLog;

import java.util.ArrayList;
import java.util.List;

public class CareLogAdapter extends RecyclerView.Adapter<CareLogAdapter.CareLogViewHolder> {

    public interface OnLogActionListener {
        void onEditLog(CareLog log);
        void onDeleteLog(CareLog log);
    }

    private List<CareLog> logs = new ArrayList<>();
    private OnLogActionListener actionListener;

    public void setLogs(List<CareLog> logs) {
        this.logs = logs != null ? logs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnLogActionListener(OnLogActionListener listener) {
        this.actionListener = listener;
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
        holder.bind(log, actionListener);
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
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public CareLogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_action_emoji);
            tvName = itemView.findViewById(R.id.tv_action_name);
            tvTime = itemView.findViewById(R.id.tv_log_time);
            tvNote = itemView.findViewById(R.id.tv_log_note);
            btnEdit = itemView.findViewById(R.id.btn_edit_log);
            btnDelete = itemView.findViewById(R.id.btn_delete_log);
        }

        public void bind(CareLog log, OnLogActionListener listener) {
            if (log.getActionType() != null) {
                tvEmoji.setText(log.getActionType().getIconEmoji());
                tvName.setText(log.getActionType().getDisplayName());
            } else {
                tvEmoji.setText("📝");
                tvName.setText("Hoạt động");
            }
            tvTime.setText(log.getRelativeTime());
            tvNote.setText(log.getNote() != null ? log.getNote() : "");

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditLog(log);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteLog(log);
                }
            });
        }
    }
}
