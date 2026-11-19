package com.example.agriflow.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Zone implements Serializable {
    private String id;
    private String name;
    private String description;
    private int rows;
    private int cols;
    private String defaultCrop;
    private long createdAt;
    private List<Cell> cells;

    public Zone() {
        // Required for Firebase
        this.cells = new ArrayList<>();
    }

    public Zone(String id, String name, String description, int rows, int cols, String defaultCrop) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rows = rows;
        this.cols = cols;
        this.defaultCrop = defaultCrop;
        this.createdAt = System.currentTimeMillis();
        this.cells = new ArrayList<>();
        initializeCells();
    }

    public void initializeCells() {
        cells.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String cellId = id + "_R" + r + "_C" + c;
                Cell cell = new Cell(cellId, id, r, c, CellStatus.HEALTHY, defaultCrop);
                cells.add(cell);
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description != null ? description : "";
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
        return defaultCrop != null ? defaultCrop : "";
    }

    public void setDefaultCrop(String defaultCrop) {
        this.defaultCrop = defaultCrop;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<Cell> getCells() {
        if (cells == null) {
            cells = new ArrayList<>();
        }
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    public Cell getCell(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) {
            return null;
        }
        int index = r * cols + c;
        if (index >= 0 && index < cells.size()) {
            return cells.get(index);
        }
        return null;
    }

    public int getTotalCells() {
        return rows * cols;
    }

    public int getHealthyCount() {
        int count = 0;
        for (Cell cell : getCells()) {
            if (cell.getStatus() == CellStatus.HEALTHY) count++;
        }
        return count;
    }

    public int getWarningCount() {
        int count = 0;
        for (Cell cell : getCells()) {
            if (cell.getStatus() == CellStatus.WARNING) count++;
        }
        return count;
    }

    public int getDiseasedCount() {
        int count = 0;
        for (Cell cell : getCells()) {
            if (cell.getStatus() == CellStatus.DISEASED) count++;
        }
        return count;
    }

    public int getEmptyCount() {
        int count = 0;
        for (Cell cell : getCells()) {
            if (cell.getStatus() == CellStatus.EMPTY) count++;
        }
        return count;
    }

    public int getDisabledCount() {
        int count = 0;
        for (Cell cell : getCells()) {
            if (cell.getStatus() == CellStatus.DISABLED) count++;
        }
        return count;
    }

    public int getActivePlantCount() {
        return getHealthyCount() + getWarningCount() + getDiseasedCount();
    }

    public int getHealthPercentage() {
        int active = getActivePlantCount();
        if (active == 0) return 100;
        return (int) Math.round((getHealthyCount() * 100.0) / active);
    }

    public String getDimensionText() {
        return rows + " dòng × " + cols + " cột (" + getTotalCells() + " ô)";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("rows", rows);
        map.put("cols", cols);
        map.put("defaultCrop", defaultCrop);
        map.put("createdAt", createdAt);
        return map;
    }

    public static Zone fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Zone zone = new Zone();
        zone.setId((String) map.get("id"));
        zone.setName((String) map.get("name"));
        zone.setDescription((String) map.get("description"));

        Object rObj = map.get("rows");
        if (rObj instanceof Number) zone.setRows(((Number) rObj).intValue());

        Object cObj = map.get("cols");
        if (cObj instanceof Number) zone.setCols(((Number) cObj).intValue());

        zone.setDefaultCrop((String) map.get("defaultCrop"));

        Object caObj = map.get("createdAt");
        if (caObj instanceof Number) zone.setCreatedAt(((Number) caObj).longValue());

        return zone;
    }
}
