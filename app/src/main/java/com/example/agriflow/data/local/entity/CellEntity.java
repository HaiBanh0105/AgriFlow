package com.example.agriflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;

@Entity(
        tableName = "cells",
        foreignKeys = @ForeignKey(
                entity = ZoneEntity.class,
                parentColumns = "id",
                childColumns = "zoneId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"zoneId", "rowIndex", "colIndex"}, unique = true),
                @Index(value = {"zoneId"}),
                @Index(value = {"isSynced"})
        }
)
public class CellEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String zoneId;
    private int rowIndex;
    private int colIndex;
    private String status;
    private String plantType;
    private String plantedDate;
    private String notes;
    private long lastWatered;
    private long lastFertilized;
    private long lastHarvested;
    private long updatedAt;
    private boolean isSynced;

    public CellEntity() {
        this.id = "";
        this.zoneId = "";
        this.status = CellStatus.EMPTY.name();
    }

    @Ignore
    public CellEntity(@NonNull String id, String zoneId, int rowIndex, int colIndex,
                      String status, String plantType, String plantedDate, String notes,
                      long lastWatered, long lastFertilized, long lastHarvested, long updatedAt, boolean isSynced) {
        this.id = id;
        this.zoneId = zoneId;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.status = status;
        this.plantType = plantType;
        this.plantedDate = plantedDate;
        this.notes = notes;
        this.lastWatered = lastWatered;
        this.lastFertilized = lastFertilized;
        this.lastHarvested = lastHarvested;
        this.updatedAt = updatedAt;
        this.isSynced = isSynced;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public int getColIndex() {
        return colIndex;
    }

    public void setColIndex(int colIndex) {
        this.colIndex = colIndex;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public String getPlantedDate() {
        return plantedDate;
    }

    public void setPlantedDate(String plantedDate) {
        this.plantedDate = plantedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getLastWatered() {
        return lastWatered;
    }

    public void setLastWatered(long lastWatered) {
        this.lastWatered = lastWatered;
    }

    public long getLastFertilized() {
        return lastFertilized;
    }

    public void setLastFertilized(long lastFertilized) {
        this.lastFertilized = lastFertilized;
    }

    public long getLastHarvested() {
        return lastHarvested;
    }

    public void setLastHarvested(long lastHarvested) {
        this.lastHarvested = lastHarvested;
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

    public static CellEntity fromDomain(Cell cell, boolean isSynced) {
        if (cell == null) return null;
        long now = System.currentTimeMillis();
        return new CellEntity(
                cell.getId(),
                cell.getZoneId(),
                cell.getRowIndex(),
                cell.getColIndex(),
                cell.getStatus() != null ? cell.getStatus().name() : CellStatus.EMPTY.name(),
                cell.getPlantType(),
                cell.getPlantedDate(),
                cell.getNotes(),
                cell.getLastWatered(),
                cell.getLastFertilized(),
                cell.getLastHarvested(),
                now,
                isSynced
        );
    }

    public Cell toDomain() {
        Cell cell = new Cell();
        cell.setId(id);
        cell.setZoneId(zoneId);
        cell.setRowIndex(rowIndex);
        cell.setColIndex(colIndex);

        CellStatus cellStatus = CellStatus.EMPTY;
        try {
            if (status != null) {
                cellStatus = CellStatus.valueOf(status);
            }
        } catch (Exception ignored) {}
        cell.setStatus(cellStatus);

        cell.setPlantType(plantType);
        cell.setPlantedDate(plantedDate);
        cell.setNotes(notes);
        cell.setLastWatered(lastWatered);
        cell.setLastFertilized(lastFertilized);
        cell.setLastHarvested(lastHarvested);
        return cell;
    }
}
