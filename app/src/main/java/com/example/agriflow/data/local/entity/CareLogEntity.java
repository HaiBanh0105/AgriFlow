package com.example.agriflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.CareLog;

@Entity(
        tableName = "care_logs",
        foreignKeys = @ForeignKey(
                entity = CellEntity.class,
                parentColumns = "id",
                childColumns = "cellId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"cellId"}),
                @Index(value = {"zoneId"}),
                @Index(value = {"isSynced"})
        }
)
public class CareLogEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String cellId;
    private String zoneId;
    private String actionType;
    private String note;
    private long timestamp;
    private boolean isSynced;

    public CareLogEntity() {
        this.id = "";
        this.cellId = "";
        this.zoneId = "";
        this.actionType = ActionType.NOTE.name();
    }

    @Ignore
    public CareLogEntity(@NonNull String id, String cellId, String zoneId,
                          String actionType, String note, long timestamp, boolean isSynced) {
        this.id = id;
        this.cellId = cellId;
        this.zoneId = zoneId;
        this.actionType = actionType;
        this.note = note;
        this.timestamp = timestamp;
        this.isSynced = isSynced;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
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

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public static CareLogEntity fromDomain(String zoneId, CareLog log, boolean isSynced) {
        if (log == null) return null;
        return new CareLogEntity(
                log.getId(),
                log.getCellId(),
                zoneId != null ? zoneId : "",
                log.getActionType() != null ? log.getActionType().name() : ActionType.NOTE.name(),
                log.getNote(),
                log.getTimestamp(),
                isSynced
        );
    }

    public CareLog toDomain() {
        ActionType type = ActionType.NOTE;
        try {
            if (actionType != null) {
                type = ActionType.valueOf(actionType);
            }
        } catch (Exception ignored) {}
        return new CareLog(id, cellId, type, note, timestamp);
    }
}
