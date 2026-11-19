package com.example.agriflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.agriflow.data.model.Zone;

@Entity(tableName = "zones")
public class ZoneEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String description;
    private int rows;
    private int cols;
    private String defaultCrop;
    private int harvestCycleDays;
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;
    private boolean isDeleted;

    public ZoneEntity() {
        this.id = "";
        this.harvestCycleDays = 30;
    }

    @Ignore
    public ZoneEntity(@NonNull String id, String name, String description, int rows, int cols,
                      String defaultCrop, int harvestCycleDays, long createdAt, long updatedAt, boolean isSynced, boolean isDeleted) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rows = rows;
        this.cols = cols;
        this.defaultCrop = defaultCrop;
        this.harvestCycleDays = harvestCycleDays > 0 ? harvestCycleDays : 30;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isSynced = isSynced;
        this.isDeleted = isDeleted;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public String getDefaultCrop() {
        return defaultCrop;
    }

    public void setDefaultCrop(String defaultCrop) {
        this.defaultCrop = defaultCrop;
    }

    public int getHarvestCycleDays() {
        return harvestCycleDays > 0 ? harvestCycleDays : 30;
    }

    public void setHarvestCycleDays(int harvestCycleDays) {
        this.harvestCycleDays = harvestCycleDays > 0 ? harvestCycleDays : 30;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public static ZoneEntity fromDomain(Zone zone, boolean isSynced) {
        if (zone == null) return null;
        long now = System.currentTimeMillis();
        return new ZoneEntity(
                zone.getId(),
                zone.getName(),
                zone.getDescription(),
                zone.getRows(),
                zone.getCols(),
                zone.getDefaultCrop(),
                zone.getHarvestCycleDays(),
                zone.getCreatedAt() > 0 ? zone.getCreatedAt() : now,
                now,
                isSynced,
                false
        );
    }

    public Zone toDomain() {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setDescription(description);
        zone.setRows(rows);
        zone.setCols(cols);
        zone.setDefaultCrop(defaultCrop);
        zone.setHarvestCycleDays(harvestCycleDays > 0 ? harvestCycleDays : 30);
        zone.setCreatedAt(createdAt);
        return zone;
    }
}
