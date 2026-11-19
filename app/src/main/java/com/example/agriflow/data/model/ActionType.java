package com.example.agriflow.data.model;

public enum ActionType {
    WATER("Tưới nước", "💧", "#0288D1"),
    FERTILIZE("Bón phân", "🌾", "#F57C00"),
    PESTICIDE("Phun thuốc", "🛡️", "#D32F2F"),
    HARVEST("Thu hoạch", "✂️", "#388E3C"),
    PRUNE("Tỉa cành", "🌿", "#689F38"),
    INSPECT("Kiểm tra", "🔍", "#7B1FA2"),
    NOTE("Ghi chú", "📝", "#607D8B");

    private final String displayName;
    private final String iconEmoji;
    private final String colorHex;

    ActionType(String displayName, String iconEmoji, String colorHex) {
        this.displayName = displayName;
        this.iconEmoji = iconEmoji;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public String getColorHex() {
        return colorHex;
    }
}
