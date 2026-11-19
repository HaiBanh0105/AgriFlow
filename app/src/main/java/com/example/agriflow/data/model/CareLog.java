package com.example.agriflow.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CareLog {
    private String id;
    private String cellId;
    private ActionType actionType;
    private String note;
    private long timestamp;

    public CareLog() {
        // Required for Firebase
    }

    public CareLog(String id, String cellId, ActionType actionType, String note, long timestamp) {
        this.id = id;
        this.cellId = cellId;
        this.actionType = actionType;
        this.note = note;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getRelativeTime() {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 30) {
            return days + " ngày trước";
        } else {
            return getFormattedTime();
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("cellId", cellId);
        map.put("actionType", actionType != null ? actionType.name() : ActionType.NOTE.name());
        map.put("note", note);
        map.put("timestamp", timestamp);
        return map;
    }

    public static CareLog fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String id = (String) map.get("id");
        String cellId = (String) map.get("cellId");
        String actionTypeStr = (String) map.get("actionType");
        ActionType type = ActionType.NOTE;
        try {
            if (actionTypeStr != null) {
                type = ActionType.valueOf(actionTypeStr);
            }
        } catch (Exception ignored) {}

        String note = (String) map.get("note");
        long timestamp = 0L;
        Object tsObj = map.get("timestamp");
        if (tsObj instanceof Number) {
            timestamp = ((Number) tsObj).longValue();
        }

        return new CareLog(id, cellId, type, note, timestamp);
    }
}
