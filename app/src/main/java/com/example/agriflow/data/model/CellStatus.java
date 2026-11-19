package com.example.agriflow.data.model;

import android.graphics.Color;

public enum CellStatus {
    HEALTHY("Khỏe mạnh", "#2E7D32", "#E8F5E9", "#81C784"),
    WARNING("Cần chăm sóc", "#E65100", "#FFF3E0", "#FFB74D"),
    DISEASED("Sâu bệnh", "#C62828", "#FFEBEE", "#E57373"),
    EMPTY("Ô trống", "#757575", "#F5F5F5", "#BDBDBD"),
    DISABLED("Lối đi / Ẩn", "#546E7A", "#ECEFF1", "#B0BEC5");

    private final String displayName;
    private final String primaryColorHex;
    private final String backgroundColorHex;
    private final String borderColorHex;

    CellStatus(String displayName, String primaryColorHex, String backgroundColorHex, String borderColorHex) {
        this.displayName = displayName;
        this.primaryColorHex = primaryColorHex;
        this.backgroundColorHex = backgroundColorHex;
        this.borderColorHex = borderColorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPrimaryColor() {
        return Color.parseColor(primaryColorHex);
    }

    public int getBackgroundColor() {
        return Color.parseColor(backgroundColorHex);
    }

    public int getBorderColor() {
        return Color.parseColor(borderColorHex);
    }
}
