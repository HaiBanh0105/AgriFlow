package com.example.agriflow.data.repository;

import android.util.Log;

import com.example.agriflow.data.firebase.FirestoreService;
import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.CareLog;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.model.Zone;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FarmRepository {
    private static final String TAG = "FarmRepository";
    private static FarmRepository instance;
    private final List<Zone> zones;
    private final FirestoreService firestoreService;

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    private final List<OnDataChangedListener> dataChangedListeners = new ArrayList<>();

    private FarmRepository() {
        zones = new ArrayList<>();
        firestoreService = FirestoreService.getInstance();
        populateSampleData();
        initFirestoreSync();
    }

    public static synchronized FarmRepository getInstance() {
        if (instance == null) {
            instance = new FarmRepository();
        }
        return instance;
    }

    public void addDataChangedListener(OnDataChangedListener listener) {
        if (listener != null && !dataChangedListeners.contains(listener)) {
            dataChangedListeners.add(listener);
        }
    }

    public void removeDataChangedListener(OnDataChangedListener listener) {
        dataChangedListeners.remove(listener);
    }

    private void notifyDataChanged() {
        for (OnDataChangedListener listener : dataChangedListeners) {
            listener.onDataChanged();
        }
    }

    private void initFirestoreSync() {
        // Listen to Firestore zones collection
        try {
            firestoreService.listenToZones(new FirestoreService.OnZonesLoadedListener() {
                @Override
                public void onZonesLoaded(List<Zone> remoteZones) {
                    if (remoteZones != null && !remoteZones.isEmpty()) {
                        Log.d(TAG, "Loaded " + remoteZones.size() + " zones from Firestore");
                        // Sync remote zones with local cache
                        for (Zone remoteZone : remoteZones) {
                            Zone local = getZoneById(remoteZone.getId());
                            if (local == null) {
                                remoteZone.initializeCells();
                                zones.add(remoteZone);
                                // Load cells from subcollection
                                loadRemoteCells(remoteZone);
                            }
                        }
                        notifyDataChanged();
                    } else {
                        Log.d(TAG, "Firestore is empty, seeding demo zones to Firestore");
                        seedDemoDataToFirestore();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.w(TAG, "Firestore sync warning (running in offline mode): " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firestore sync: ", e);
        }
    }

    private void loadRemoteCells(Zone zone) {
        firestoreService.listenToZoneCells(zone.getId(), new FirestoreService.OnCellsLoadedListener() {
            @Override
            public void onCellsLoaded(List<Cell> cells) {
                if (cells != null && !cells.isEmpty()) {
                    for (Cell cell : cells) {
                        Cell existing = zone.getCell(cell.getRowIndex(), cell.getColIndex());
                        if (existing != null) {
                            existing.setStatus(cell.getStatus());
                            existing.setPlantType(cell.getPlantType());
                            existing.setPlantedDate(cell.getPlantedDate());
                            existing.setNotes(cell.getNotes());
                            existing.setLastWatered(cell.getLastWatered());
                            existing.setLastFertilized(cell.getLastFertilized());
                        }
                    }
                    notifyDataChanged();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Could not load subcollection cells: " + e.getMessage());
            }
        });
    }

    private void seedDemoDataToFirestore() {
        for (Zone zone : zones) {
            firestoreService.saveZone(zone);
        }
    }

    private void populateSampleData() {
        // Zone 1: Nhà màng Cà Chua Bi
        Zone zone1 = new Zone(
                "zone_1",
                "Khu A - Nhà Màng Cà Chua Bi",
                "Hệ thống tưới nhỏ giọt Isarel, gieo trồng cà chua bi chất lượng cao",
                8,
                10,
                "Cà chua bi Nhật"
        );

        // Row 3: Pathway / Obstacle (Cột nhà màng & Đường ống cấp nước)
        for (int c = 4; c <= 5; c++) {
            Cell cell = zone1.getCell(3, c);
            if (cell != null) {
                cell.setStatus(CellStatus.DISABLED);
                cell.setPlantType("Lối đi kỹ thuật");
            }
        }

        // Row 2 Col 2, 3: Diseased (Phấn trắng)
        Cell diseased1 = zone1.getCell(2, 2);
        if (diseased1 != null) {
            diseased1.setStatus(CellStatus.DISEASED);
            diseased1.setNotes("Phát hiện nấm phấn trắng mặt dưới lá");
            diseased1.addCareLog(new CareLog(
                    UUID.randomUUID().toString(),
                    diseased1.getId(),
                    ActionType.PESTICIDE,
                    "Phun chế phẩm sinh học Bacillus subtilis liều 2ml/L",
                    System.currentTimeMillis() - 3600000L * 5
            ));
        }

        Cell diseased2 = zone1.getCell(2, 3);
        if (diseased2 != null) {
            diseased2.setStatus(CellStatus.DISEASED);
            diseased2.setNotes("Lây từ ô C2, ngọn hơi héo");
            diseased2.addCareLog(new CareLog(
                    UUID.randomUUID().toString(),
                    diseased2.getId(),
                    ActionType.INSPECT,
                    "Cách ly cành bệnh, kiểm tra hệ thống thông gió",
                    System.currentTimeMillis() - 3600000L * 4
            ));
        }

        // Row 5: Needs care (Warning - Cần bổ sung đạm hữu cơ)
        for (int c = 2; c <= 6; c++) {
            Cell warningCell = zone1.getCell(5, c);
            if (warningCell != null) {
                warningCell.setStatus(CellStatus.WARNING);
                warningCell.setNotes("Độ ẩm giá thể thấp, lá hơi rũ lúc trưa");
                warningCell.addCareLog(new CareLog(
                        UUID.randomUUID().toString(),
                        warningCell.getId(),
                        ActionType.WATER,
                        "Tưới nhỏ giọt bổ sung 500ml/gốc",
                        System.currentTimeMillis() - 3600000L * 12
                ));
            }
        }

        // Row 7 Col 8, 9: Empty (Đã thu hoạch)
        for (int c = 8; c <= 9; c++) {
            Cell emptyCell = zone1.getCell(7, c);
            if (emptyCell != null) {
                emptyCell.setStatus(CellStatus.EMPTY);
                emptyCell.setPlantType("");
                emptyCell.setNotes("Đã thu hoạch 4.5kg, chuẩn bị xử lý đất");
                emptyCell.addCareLog(new CareLog(
                        UUID.randomUUID().toString(),
                        emptyCell.getId(),
                        ActionType.HARVEST,
                        "Thu hoạch đợt 1, chất lượng trái đồng đều",
                        System.currentTimeMillis() - 86400000L * 2
                ));
            }
        }

        // Add some logs to a healthy cell
        Cell healthySample = zone1.getCell(0, 0);
        if (healthySample != null) {
            healthySample.addCareLog(new CareLog(
                    UUID.randomUUID().toString(),
                    healthySample.getId(),
                    ActionType.WATER,
                    "Tưới dinh dưỡng AB định kỳ buổi sáng",
                    System.currentTimeMillis() - 3600000L * 3
            ));
            healthySample.addCareLog(new CareLog(
                    UUID.randomUUID().toString(),
                    healthySample.getId(),
                    ActionType.FERTILIZE,
                    "Bổ sung Canxi-Bo chống rụng hoa",
                    System.currentTimeMillis() - 86400000L
            ));
        }

        zones.add(zone1);

        // Zone 2: Vườn Dưa Lưới VietGAP
        Zone zone2 = new Zone(
                "zone_2",
                "Khu B - Dưa Lưới VietGAP",
                "Vườn giàn treo nhà kính khép kín, chuẩn bị giai đoạn tạo ngọt",
                6,
                12,
                "Dưa lưới Huỳnh Long"
        );
        for (int c = 9; c <= 11; c++) {
            Cell cell = zone2.getCell(1, c);
            if (cell != null) {
                cell.setStatus(CellStatus.WARNING);
                cell.setNotes("Theo dõi bọ trĩ ở tầng lá ngọn");
            }
        }
        zones.add(zone2);

        // Zone 3: Dâu Tây New Zealand
        Zone zone3 = new Zone(
                "zone_3",
                "Khu C - Luống Dâu Tây New Zealand",
                "Giàn giá thể xơ dừa cao 1m, hệ thống quạt đối lưu làm mát",
                5,
                8,
                "Dâu tây New Zealand F1"
        );
        Cell disC = zone3.getCell(1, 1);
        if (disC != null) {
            disC.setStatus(CellStatus.DISEASED);
            disC.setNotes("Nghi ngờ thán thư cổ rễ");
        }
        zones.add(zone3);
    }

    public List<Zone> getZones() {
        return new ArrayList<>(zones);
    }

    public Zone getZoneById(String id) {
        for (Zone z : zones) {
            if (z.getId().equals(id)) {
                return z;
            }
        }
        return null;
    }

    public void addZone(Zone zone) {
        zones.add(0, zone);
        firestoreService.saveZone(zone);
        notifyDataChanged();
    }

    public void updateZone(Zone updatedZone) {
        for (int i = 0; i < zones.size(); i++) {
            if (zones.get(i).getId().equals(updatedZone.getId())) {
                zones.set(i, updatedZone);
                firestoreService.saveZone(updatedZone);
                notifyDataChanged();
                return;
            }
        }
    }

    public void deleteZone(String zoneId) {
        zones.removeIf(z -> z.getId().equals(zoneId));
        firestoreService.deleteZone(zoneId);
        notifyDataChanged();
    }

    public void updateCell(String zoneId, Cell updatedCell) {
        Zone zone = getZoneById(zoneId);
        if (zone != null) {
            Cell existing = zone.getCell(updatedCell.getRowIndex(), updatedCell.getColIndex());
            if (existing != null) {
                existing.setStatus(updatedCell.getStatus());
                existing.setPlantType(updatedCell.getPlantType());
                existing.setPlantedDate(updatedCell.getPlantedDate());
                existing.setNotes(updatedCell.getNotes());
                existing.setLastWatered(updatedCell.getLastWatered());
                existing.setLastFertilized(updatedCell.getLastFertilized());
                firestoreService.updateCell(zoneId, existing);
                notifyDataChanged();
            }
        }
    }

    public void bulkUpdateStatus(String zoneId, List<Cell> targetCells, CellStatus newStatus) {
        Zone zone = getZoneById(zoneId);
        if (zone == null || targetCells == null) return;

        for (Cell target : targetCells) {
            Cell cell = zone.getCell(target.getRowIndex(), target.getColIndex());
            if (cell != null) {
                cell.setStatus(newStatus);
            }
        }
        firestoreService.bulkUpdateStatus(zoneId, targetCells, newStatus);
        notifyDataChanged();
    }

    public void bulkPerformAction(String zoneId, List<Cell> targetCells, ActionType actionType, String note) {
        Zone zone = getZoneById(zoneId);
        if (zone == null || targetCells == null) return;

        long now = System.currentTimeMillis();
        for (Cell target : targetCells) {
            Cell cell = zone.getCell(target.getRowIndex(), target.getColIndex());
            if (cell != null) {
                if (actionType == ActionType.WATER) {
                    cell.setLastWatered(now);
                    if (cell.getStatus() == CellStatus.WARNING) {
                        cell.setStatus(CellStatus.HEALTHY);
                    }
                } else if (actionType == ActionType.FERTILIZE) {
                    cell.setLastFertilized(now);
                } else if (actionType == ActionType.HARVEST) {
                    cell.setStatus(CellStatus.EMPTY);
                }
                cell.addCareLog(new CareLog(
                        UUID.randomUUID().toString(),
                        cell.getId(),
                        actionType,
                        note,
                        now
                ));
            }
        }
        firestoreService.bulkPerformAction(zoneId, targetCells, actionType, note);
        notifyDataChanged();
    }

    public int getTotalZonesCount() {
        return zones.size();
    }

    public int getTotalPlantsCount() {
        int total = 0;
        for (Zone z : zones) {
            total += z.getActivePlantCount();
        }
        return total;
    }

    public int getOverallHealthyPercentage() {
        int healthy = 0;
        int active = 0;
        for (Zone z : zones) {
            healthy += z.getHealthyCount();
            active += z.getActivePlantCount();
        }
        if (active == 0) return 100;
        return (int) Math.round((healthy * 100.0) / active);
    }

    public int getTotalIssueCount() {
        int issues = 0;
        for (Zone z : zones) {
            issues += z.getWarningCount() + z.getDiseasedCount();
        }
        return issues;
    }
}
