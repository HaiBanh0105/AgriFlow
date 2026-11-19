package com.example.agriflow.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cell {
    private String id;
    private String zoneId;
    private int rowIndex;
    private int colIndex;
    private CellStatus status;
    private String plantType;
    private String plantedDate;
    private String notes;
    private long lastWatered;
    private long lastFertilized;
    private long lastHarvested;
    private List<CareLog> careLogs;

    public Cell() {
        // Required for Firebase
        this.careLogs = new ArrayList<>();
    }

    public Cell(String id, String zoneId, int rowIndex, int colIndex, CellStatus status, String plantType) {
        this.id = id;
        this.zoneId = zoneId;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.status = status;
        this.plantType = plantType;
        this.plantedDate = "15/10/2026";
        this.notes = "";
        this.lastWatered = System.currentTimeMillis() - 86400000L;
        this.lastFertilized = System.currentTimeMillis() - 86400000L * 3;
        this.lastHarvested = 0L;
        this.careLogs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public CellStatus getStatus() {
        return status != null ? status : CellStatus.EMPTY;
    }

    public void setStatus(CellStatus status) {
        this.status = status;
    }

    public String getPlantType() {
        return plantType != null ? plantType : "";
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public String getPlantedDate() {
        return plantedDate != null ? plantedDate : "";
    }

    public void setPlantedDate(String plantedDate) {
        this.plantedDate = plantedDate;
    }

    public String getNotes() {
        return notes != null ? notes : "";
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

    public int getDaysSinceLastHarvest() {
        if (lastHarvested <= 0) return -1;
        long diff = System.currentTimeMillis() - lastHarvested;
        return Math.max(0, (int) (diff / 86400000L));
    }

    public void recalculateMetricsFromLogs() {
        if (careLogs == null) return;
        long maxWater = 0;
        long maxFertilize = 0;
        long maxHarvest = 0;
        for (CareLog log : careLogs) {
            if (log.getActionType() == ActionType.WATER && log.getTimestamp() > maxWater) {
                maxWater = log.getTimestamp();
            } else if (log.getActionType() == ActionType.FERTILIZE && log.getTimestamp() > maxFertilize) {
                maxFertilize = log.getTimestamp();
            } else if (log.getActionType() == ActionType.HARVEST && log.getTimestamp() > maxHarvest) {
                maxHarvest = log.getTimestamp();
            }
        }
        if (maxWater > 0) this.lastWatered = maxWater;
        if (maxFertilize > 0) this.lastFertilized = maxFertilize;
        this.lastHarvested = maxHarvest; // 0 if none
    }

    public void removeCareLog(String logId) {
        if (careLogs == null || logId == null) return;
        careLogs.removeIf(l -> logId.equals(l.getId()));
        recalculateMetricsFromLogs();
    }

    public void updateCareLog(CareLog updated) {
        if (careLogs == null || updated == null) return;
        for (int i = 0; i < careLogs.size(); i++) {
            if (careLogs.get(i).getId().equals(updated.getId())) {
                careLogs.set(i, updated);
                break;
            }
        }
        recalculateMetricsFromLogs();
    }

    public List<CareLog> getCareLogs() {
        if (careLogs == null) {
            careLogs = new ArrayList<>();
        }
        return careLogs;
    }

    public void setCareLogs(List<CareLog> careLogs) {
        this.careLogs = careLogs;
        recalculateMetricsFromLogs();
    }

    public void addCareLog(CareLog log) {
        if (log == null) return;
        if (careLogs == null) {
            careLogs = new ArrayList<>();
        }
        for (CareLog existing : careLogs) {
            if (existing.getId() != null && existing.getId().equals(log.getId())) {
                return; // Prevent duplicate insertion of identical log
            }
        }
        careLogs.add(0, log);
        if (log.getActionType() == ActionType.HARVEST) {
            if (log.getTimestamp() > this.lastHarvested) {
                this.lastHarvested = log.getTimestamp();
            }
        }
    }

    public String getCoordinateLabel() {
        return "Hàng " + (rowIndex + 1) + " • Cột " + getColLetter(colIndex);
    }

    public static String getColLetter(int col) {
        if (col < 26) {
            return String.valueOf((char) ('A' + col));
        } else {
            return String.valueOf(col + 1);
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("zoneId", zoneId);
        map.put("rowIndex", rowIndex);
        map.put("colIndex", colIndex);
        map.put("status", status != null ? status.name() : CellStatus.EMPTY.name());
        map.put("plantType", plantType);
        map.put("plantedDate", plantedDate);
        map.put("notes", notes);
        map.put("lastWatered", lastWatered);
        map.put("lastFertilized", lastFertilized);
        map.put("lastHarvested", lastHarvested);
        return map;
    }

    public static Cell fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Cell cell = new Cell();
        cell.setId((String) map.get("id"));
        cell.setZoneId((String) map.get("zoneId"));

        Object rObj = map.get("rowIndex");
        if (rObj instanceof Number) cell.setRowIndex(((Number) rObj).intValue());

        Object cObj = map.get("colIndex");
        if (cObj instanceof Number) cell.setColIndex(((Number) cObj).intValue());

        String statusStr = (String) map.get("status");
        CellStatus status = CellStatus.EMPTY;
        try {
            if (statusStr != null) status = CellStatus.valueOf(statusStr);
        } catch (Exception ignored) {}
        cell.setStatus(status);

        cell.setPlantType((String) map.get("plantType"));
        cell.setPlantedDate((String) map.get("plantedDate"));
        cell.setNotes((String) map.get("notes"));

        Object lwObj = map.get("lastWatered");
        if (lwObj instanceof Number) cell.setLastWatered(((Number) lwObj).longValue());

        Object lfObj = map.get("lastFertilized");
        if (lfObj instanceof Number) cell.setLastFertilized(((Number) lfObj).longValue());

        Object lhObj = map.get("lastHarvested");
        if (lhObj instanceof Number) cell.setLastHarvested(((Number) lhObj).longValue());

        return cell;
    }
}
