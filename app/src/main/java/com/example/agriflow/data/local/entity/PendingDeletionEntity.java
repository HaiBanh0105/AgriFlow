package com.example.agriflow.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_deletions")
public class PendingDeletionEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String entityType; // "ZONE", "CELL", "CARE_LOG"
    private String zoneId;
    private String cellId;
    private String targetId;
    private long createdAt;

    public PendingDeletionEntity() {
    }

    @Ignore
    public PendingDeletionEntity(String entityType, String zoneId, String cellId, String targetId, long createdAt) {
        this.entityType = entityType;
        this.zoneId = zoneId;
        this.cellId = cellId;
        this.targetId = targetId;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
