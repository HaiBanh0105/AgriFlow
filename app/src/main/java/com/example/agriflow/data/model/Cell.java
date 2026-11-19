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

    public List<CareLog> getCareLogs() {
        if (careLogs == null) {
            careLogs = new ArrayList<>();
        }
        return careLogs;
    }

    public void setCareLogs(List<CareLog> careLogs) {
        this.careLogs = careLogs;
    }

    public void addCareLog(CareLog log) {
        if (careLogs == null) {
            careLogs = new ArrayList<>();
        }
        careLogs.add(0, log);
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

        return cell;
    }
}
