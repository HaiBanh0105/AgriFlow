package com.example.agriflow.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.agriflow.data.firebase.FirestoreService;
import com.example.agriflow.data.local.AppDatabase;
import com.example.agriflow.data.local.dao.CareLogDao;
import com.example.agriflow.data.local.dao.CellDao;
import com.example.agriflow.data.local.dao.ZoneDao;
import com.example.agriflow.data.local.entity.CareLogEntity;
import com.example.agriflow.data.local.entity.CellEntity;
import com.example.agriflow.data.local.entity.PendingDeletionEntity;
import com.example.agriflow.data.local.entity.ZoneEntity;
import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.CareLog;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.model.Zone;
import com.example.agriflow.data.sync.SyncManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FarmRepository {
    private static final String TAG = "FarmRepository";
    private static volatile FarmRepository instance;

    private final List<Zone> zones = new ArrayList<>();
    private final List<OnDataChangedListener> dataChangedListeners = new ArrayList<>();
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();

    private Context appContext;
    private AppDatabase localDb;
    private SyncManager syncManager;
    private FirestoreService firestoreService;

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    private FarmRepository(Context context) {
        if (context != null) {
            this.appContext = context.getApplicationContext();
            try {
                this.localDb = AppDatabase.getInstance(this.appContext);
                this.syncManager = SyncManager.getInstance(this.appContext);
            } catch (Exception e) {
                Log.w(TAG, "Database init warning: " + e.getMessage());
            }
        }
        try {
            this.firestoreService = FirestoreService.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "Firestore init warning: " + e.getMessage());
        }
        initializeData();
    }

    public static FarmRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FarmRepository.class) {
                if (instance == null) {
                    instance = new FarmRepository(context);
                }
            }
        }
        return instance;
    }

    public static FarmRepository getInstance() {
        if (instance == null) {
            synchronized (FarmRepository.class) {
                if (instance == null) {
                    instance = new FarmRepository(null);
                }
            }
        }
        return instance;
    }

    public void setContext(Context context) {
        if (this.appContext == null && context != null) {
            this.appContext = context.getApplicationContext();
            this.localDb = AppDatabase.getInstance(this.appContext);
            this.syncManager = SyncManager.getInstance(this.appContext);
            this.firestoreService = FirestoreService.getInstance();
            loadFromRoomOrSeed();
        }
    }

    private void initializeData() {
        if (localDb != null) {
            loadFromRoomOrSeed();
        } else {
            populateSampleData();
        }
    }

    private void loadFromRoomOrSeed() {
        diskExecutor.execute(() -> {
            try {
                ZoneDao zoneDao = localDb.zoneDao();
                CellDao cellDao = localDb.cellDao();
                CareLogDao careLogDao = localDb.careLogDao();

                List<ZoneEntity> zoneEntities = zoneDao.getAllZones();
                if (zoneEntities != null && !zoneEntities.isEmpty()) {
                    Log.d(TAG, "Loading " + zoneEntities.size() + " zones from Room database");
                    List<Zone> loadedZones = new ArrayList<>();
                    for (ZoneEntity ze : zoneEntities) {
                        Zone zone = ze.toDomain();
                        List<CellEntity> cellEntities = cellDao.getCellsByZone(zone.getId());
                        List<Cell> cells = new ArrayList<>();
                        for (CellEntity ce : cellEntities) {
                            Cell cell = ce.toDomain();
                            List<CareLogEntity> logEntities = careLogDao.getLogsByCell(cell.getId());
                            for (CareLogEntity le : logEntities) {
                                cell.addCareLog(le.toDomain());
                            }
                            cells.add(cell);
                        }
                        zone.setCells(cells);
                        loadedZones.add(zone);
                    }
                    synchronized (zones) {
                        zones.clear();
                        zones.addAll(loadedZones);
                    }
                    notifyDataChanged();
                } else {
                    Log.d(TAG, "Room is empty. Seeding initial demo zones to Room database");
                    populateSampleData();
                    saveSampleDataToRoom();
                    notifyDataChanged();
                }

                if (syncManager != null) {
                    syncManager.triggerSync();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in loadFromRoomOrSeed: ", e);
                populateSampleData();
                notifyDataChanged();
            }
        });
    }

    private void saveSampleDataToRoom() {
        if (localDb == null) return;
        diskExecutor.execute(() -> {
            try {
                ZoneDao zoneDao = localDb.zoneDao();
                CellDao cellDao = localDb.cellDao();
                CareLogDao careLogDao = localDb.careLogDao();

                for (Zone zone : zones) {
                    zoneDao.insert(ZoneEntity.fromDomain(zone, false));

                    List<CellEntity> cellEntities = new ArrayList<>();
                    for (Cell cell : zone.getCells()) {
                        cellEntities.add(CellEntity.fromDomain(cell, false));
                        for (CareLog log : cell.getCareLogs()) {
                            careLogDao.insert(CareLogEntity.fromDomain(zone.getId(), log, false));
                        }
                    }
                    cellDao.insertAll(cellEntities);
                }
                Log.d(TAG, "Successfully seeded demo data into Room database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving sample data to Room: ", e);
            }
        });
    }

    private void populateSampleData() {
        synchronized (zones) {
            zones.clear();

            // Zone 1: Nhà màng Cà Chua Bi
            Zone zone1 = new Zone(
                    "zone_1",
                    "Khu A - Nhà Màng Cà Chua Bi",
                    "Hệ thống tưới nhỏ giọt Isarel, gieo trồng cà chua bi chất lượng cao",
                    8,
                    10,
                    "Cà chua bi Nhật"
            );

            // Row 3: Pathway / Obstacle
            for (int c = 4; c <= 5; c++) {
                Cell cell = zone1.getCell(3, c);
                if (cell != null) {
                    cell.setStatus(CellStatus.DISABLED);
                    cell.setPlantType("Lối đi kỹ thuật");
                }
            }

            // Diseased cells
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

            // Warning cells
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

            // Empty cells
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

            // Healthy sample with logs
            Cell healthySample = zone1.getCell(0, 0);
            if (healthySample != null) {
                healthySample.addCareLog(new CareLog(
                        UUID.randomUUID().toString(),
                        healthySample.getId(),
                        ActionType.WATER,
                        "Tưới dinh dưỡng AB định kỳ buổi sáng",
                        System.currentTimeMillis() - 3600000L * 3
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

            // Zone 4: Vườn Dừa Xiêm Lùn Bến Tre (Chuyên canh dừa & chu kỳ thu hoạch 28 ngày)
            Zone zoneCoconut = new Zone(
                    "zone_coconut",
                    "Khu Dừa - Dừa Xiêm Lùn Bến Tre",
                    "Vườn dừa xiêm xanh chuyên canh chuẩn hữu cơ, chu kỳ thu hoạch 28 ngày/lứa",
                    6,
                    8,
                    "Dừa xiêm xanh",
                    28
            );
            long dayMs = 86400000L;
            long nowMs = System.currentTimeMillis();

            // Nhóm 1: Vừa thu hoạch 5 ngày trước
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 3; c++) {
                    Cell cell = zoneCoconut.getCell(r, c);
                    if (cell != null) {
                        cell.setLastHarvested(nowMs - 5 * dayMs);
                        cell.addCareLog(new CareLog(
                                UUID.randomUUID().toString(),
                                cell.getId(),
                                ActionType.HARVEST,
                                "Thu hoạch đợt vừa rồi: 16 trái/buồng, cơm ngọt",
                                nowMs - 5 * dayMs
                        ));
                    }
                }
            }

            // Nhóm 2: Đã thu hoạch 20 ngày trước (đang nuôi trái)
            for (int r = 2; r < 4; r++) {
                for (int c = 0; c < 3; c++) {
                    Cell cell = zoneCoconut.getCell(r, c);
                    if (cell != null) {
                        cell.setLastHarvested(nowMs - 20 * dayMs);
                        cell.addCareLog(new CareLog(
                                UUID.randomUUID().toString(),
                                cell.getId(),
                                ActionType.HARVEST,
                                "Thu hoạch đợt trước: 14 trái, cơm mỏng",
                                nowMs - 20 * dayMs
                        ));
                    }
                }
            }

            // Nhóm 3: Đã 32 ngày kể từ lần thu trước -> ĐẾN KỲ THU HOẠCH!
            for (int r = 0; r < 4; r++) {
                for (int c = 4; c < 7; c++) {
                    Cell cell = zoneCoconut.getCell(r, c);
                    if (cell != null) {
                        cell.setLastHarvested(nowMs - 32 * dayMs);
                        cell.setNotes("Trái đã già, cần đi hái đợt mới!");
                        cell.addCareLog(new CareLog(
                                UUID.randomUUID().toString(),
                                cell.getId(),
                                ActionType.HARVEST,
                                "Đợt thu hoạch cũ (32 ngày trước)",
                                nowMs - 32 * dayMs
                        ));
                    }
                }
            }

            // Hàng 5: Lối đi vận chuyển
            for (int c = 0; c < 8; c++) {
                Cell aisle = zoneCoconut.getCell(5, c);
                if (aisle != null) {
                    aisle.setStatus(CellStatus.DISABLED);
                }
            }

            zones.add(0, zoneCoconut); // Đưa khu dừa lên đầu danh sách!
        }
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

    public List<Zone> getZones() {
        synchronized (zones) {
            return new ArrayList<>(zones);
        }
    }

    public Zone getZoneById(String id) {
        synchronized (zones) {
            for (Zone z : zones) {
                if (z.getId().equals(id)) {
                    return z;
                }
            }
        }
        return null;
    }

    public void addZone(Zone zone) {
        synchronized (zones) {
            zones.add(0, zone);
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.zoneDao().insert(ZoneEntity.fromDomain(zone, false));
                    List<CellEntity> cellEntities = new ArrayList<>();
                    for (Cell cell : zone.getCells()) {
                        cellEntities.add(CellEntity.fromDomain(cell, false));
                    }
                    localDb.cellDao().insertAll(cellEntities);
                } catch (Exception e) {
                    Log.e(TAG, "Error inserting zone to Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.saveZone(zone);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void updateZone(Zone updatedZone) {
        synchronized (zones) {
            for (int i = 0; i < zones.size(); i++) {
                if (zones.get(i).getId().equals(updatedZone.getId())) {
                    zones.set(i, updatedZone);
                    break;
                }
            }
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.zoneDao().update(ZoneEntity.fromDomain(updatedZone, false));
                } catch (Exception e) {
                    Log.e(TAG, "Error updating zone in Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.saveZone(updatedZone);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void updateZoneInfo(String zoneId, String name, String description, String defaultCrop, int harvestCycleDays) {
        Zone zone = getZoneById(zoneId);
        if (zone == null) return;
        updateZoneInfo(zoneId, name, description, defaultCrop, harvestCycleDays, zone.getRows(), zone.getCols());
    }

    public void updateZoneInfo(String zoneId, String name, String description, String defaultCrop, int harvestCycleDays, int newRows, int newCols) {
        Zone zone = getZoneById(zoneId);
        if (zone == null) return;

        if (newRows < 1) newRows = 1;
        if (newCols < 1) newCols = 1;

        zone.setName(name);
        zone.setDescription(description);
        zone.setDefaultCrop(defaultCrop);
        zone.setHarvestCycleDays(harvestCycleDays);

        int oldRows = zone.getRows();
        int oldCols = zone.getCols();
        boolean dimensionChanged = (oldRows != newRows || oldCols != newCols);

        List<CellEntity> newEntities = new ArrayList<>();
        final List<String> trimmedCellIds = new ArrayList<>();

        if (dimensionChanged) {
            for (Cell oldCell : zone.getCells()) {
                if (oldCell.getRowIndex() >= newRows || oldCell.getColIndex() >= newCols) {
                    trimmedCellIds.add(oldCell.getId());
                }
            }

            Map<String, Cell> existingMap = new HashMap<>();
            for (Cell cell : zone.getCells()) {
                existingMap.put(cell.getRowIndex() + "_" + cell.getColIndex(), cell);
            }

            List<Cell> newCells = new ArrayList<>();
            for (int r = 0; r < newRows; r++) {
                for (int c = 0; c < newCols; c++) {
                    String key = r + "_" + c;
                    Cell existing = existingMap.get(key);
                    if (existing != null) {
                        newCells.add(existing);
                        newEntities.add(CellEntity.fromDomain(existing, false));
                    } else {
                        String cellId = zoneId + "_R" + r + "_C" + c;
                        Cell newCell = new Cell(cellId, zoneId, r, c, CellStatus.HEALTHY, defaultCrop);
                        newCells.add(newCell);
                        newEntities.add(CellEntity.fromDomain(newCell, false));
                    }
                }
            }

            zone.setRows(newRows);
            zone.setCols(newCols);
            zone.setCells(newCells);
        }

        final boolean finalDimChanged = dimensionChanged;

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.zoneDao().update(ZoneEntity.fromDomain(zone, false));
                    if (finalDimChanged) {
                        for (String trimmedId : trimmedCellIds) {
                            localDb.pendingDeletionDao().insert(new PendingDeletionEntity("CELL", zoneId, null, trimmedId, System.currentTimeMillis()));
                        }
                        localDb.cellDao().deleteByZone(zoneId);
                        localDb.cellDao().insertAll(newEntities);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating zone info in Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            if (!trimmedCellIds.isEmpty()) {
                firestoreService.deleteCells(zoneId, trimmedCellIds);
            }
            firestoreService.updateZoneInfo(zoneId, name, description, defaultCrop, harvestCycleDays, newRows, newCols);
            firestoreService.saveZone(zone);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void deleteZone(String zoneId) {
        synchronized (zones) {
            zones.removeIf(z -> z.getId().equals(zoneId));
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.pendingDeletionDao().insert(new PendingDeletionEntity("ZONE", zoneId, null, zoneId, System.currentTimeMillis()));
                    localDb.careLogDao().deleteByZone(zoneId);
                    localDb.cellDao().deleteByZone(zoneId);
                    localDb.zoneDao().deleteById(zoneId);
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting zone from Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.deleteZone(zoneId);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

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
            }
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.cellDao().update(CellEntity.fromDomain(updatedCell, false));
                } catch (Exception e) {
                    Log.e(TAG, "Error updating cell in Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.updateCell(zoneId, updatedCell);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void bulkUpdateStatus(String zoneId, List<Cell> targetCells, CellStatus newStatus) {
        Zone zone = getZoneById(zoneId);
        if (zone == null || targetCells == null) return;

        List<CellEntity> entitiesToUpdate = new ArrayList<>();
        for (Cell target : targetCells) {
            Cell cell = zone.getCell(target.getRowIndex(), target.getColIndex());
            if (cell != null) {
                cell.setStatus(newStatus);
                if (newStatus == CellStatus.DISABLED) {
                    cell.setPlantType("Lối đi");
                } else if ("Lối đi".equals(cell.getPlantType()) || "Lối đi kỹ thuật".equals(cell.getPlantType()) || cell.getPlantType() == null || cell.getPlantType().isEmpty()) {
                    cell.setPlantType(zone.getDefaultCrop());
                }
                entitiesToUpdate.add(CellEntity.fromDomain(cell, false));
            }
        }

        if (localDb != null && !entitiesToUpdate.isEmpty()) {
            diskExecutor.execute(() -> {
                try {
                    localDb.cellDao().updateAll(entitiesToUpdate);
                } catch (Exception e) {
                    Log.e(TAG, "Error in bulk update status in Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.bulkUpdateStatus(zoneId, targetCells, newStatus);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void bulkPerformAction(String zoneId, List<Cell> targetCells, ActionType actionType, String note) {
        Zone zone = getZoneById(zoneId);
        if (zone == null || targetCells == null) return;

        long now = System.currentTimeMillis();
        List<CellEntity> updatedCells = new ArrayList<>();
        List<CareLogEntity> newLogs = new ArrayList<>();

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
                    cell.setLastHarvested(now);
                    cell.setStatus(CellStatus.EMPTY);
                    cell.setPlantType("");
                } else if (actionType == ActionType.PESTICIDE) {
                    if (cell.getStatus() == CellStatus.DISEASED) {
                        cell.setStatus(CellStatus.WARNING);
                    }
                }

                CareLog log = new CareLog(UUID.randomUUID().toString(), cell.getId(), actionType, note, now);
                cell.addCareLog(log);

                updatedCells.add(CellEntity.fromDomain(cell, false));
                newLogs.add(CareLogEntity.fromDomain(zoneId, log, false));
            }
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    if (!updatedCells.isEmpty()) {
                        localDb.cellDao().updateAll(updatedCells);
                    }
                    if (!newLogs.isEmpty()) {
                        localDb.careLogDao().insertAll(newLogs);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving bulk action to Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.bulkPerformAction(zoneId, targetCells, actionType, note);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void addCareLog(String zoneId, String cellId, CareLog log) {
        Zone zone = getZoneById(zoneId);
        if (zone != null) {
            for (Cell cell : zone.getCells()) {
                if (cell.getId().equals(cellId)) {
                    cell.addCareLog(log);
                    break;
                }
            }
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.careLogDao().insert(CareLogEntity.fromDomain(zoneId, log, false));
                } catch (Exception e) {
                    Log.e(TAG, "Error inserting CareLog to Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.addCareLog(zoneId, cellId, log);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void bulkPlantCrops(String zoneId, List<Cell> targetCells, String cropName, String note) {
        Zone zone = getZoneById(zoneId);
        if (zone == null || targetCells == null) return;

        long now = System.currentTimeMillis();
        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date(now));
        List<CellEntity> updatedCells = new ArrayList<>();
        List<CareLogEntity> newLogs = new ArrayList<>();

        for (Cell target : targetCells) {
            Cell cell = zone.getCell(target.getRowIndex(), target.getColIndex());
            if (cell != null) {
                cell.setPlantType(cropName);
                cell.setPlantedDate(dateStr);
                cell.setStatus(CellStatus.HEALTHY);

                CareLog log = new CareLog(UUID.randomUUID().toString(), cell.getId(), ActionType.PLANT, note != null && !note.isEmpty() ? note : "Gieo trồng giống " + cropName, now);
                cell.addCareLog(log);

                updatedCells.add(CellEntity.fromDomain(cell, false));
                newLogs.add(CareLogEntity.fromDomain(zoneId, log, false));
            }
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    if (!updatedCells.isEmpty()) {
                        localDb.cellDao().updateAll(updatedCells);
                    }
                    if (!newLogs.isEmpty()) {
                        localDb.careLogDao().insertAll(newLogs);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving bulk plant to Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.bulkPlantCrops(zoneId, targetCells, cropName, note);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void bulkHarvest(String zoneId, List<Cell> targetCells, String yieldNote, boolean keepTree) {
        Zone zone = getZoneById(zoneId);
        if (zone == null || targetCells == null) return;

        long now = System.currentTimeMillis();
        List<CellEntity> updatedCells = new ArrayList<>();
        List<CareLogEntity> newLogs = new ArrayList<>();

        for (Cell target : targetCells) {
            Cell cell = zone.getCell(target.getRowIndex(), target.getColIndex());
            if (cell != null) {
                cell.setLastHarvested(now);
                if (keepTree) {
                    if (cell.getStatus() == CellStatus.WARNING) {
                        cell.setStatus(CellStatus.HEALTHY);
                    }
                } else {
                    cell.setStatus(CellStatus.EMPTY);
                    cell.setPlantType("");
                }

                CareLog log = new CareLog(UUID.randomUUID().toString(), cell.getId(), ActionType.HARVEST, yieldNote, now);
                cell.addCareLog(log);

                updatedCells.add(CellEntity.fromDomain(cell, false));
                newLogs.add(CareLogEntity.fromDomain(zoneId, log, false));
            }
        }

        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    if (!updatedCells.isEmpty()) {
                        localDb.cellDao().updateAll(updatedCells);
                    }
                    if (!newLogs.isEmpty()) {
                        localDb.careLogDao().insertAll(newLogs);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving bulk harvest to Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.bulkHarvest(zoneId, targetCells, yieldNote, keepTree);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void updateCareLog(String zoneId, String cellId, CareLog updatedLog) {
        Zone zone = getZoneById(zoneId);
        Cell targetCell = null;
        if (zone != null) {
            for (Cell cell : zone.getCells()) {
                if (cell.getId().equals(cellId)) {
                    cell.updateCareLog(updatedLog);
                    targetCell = cell;
                    break;
                }
            }
        }

        final Cell cellToSave = targetCell;
        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.careLogDao().updateLog(
                            updatedLog.getId(),
                            updatedLog.getActionType().name(),
                            updatedLog.getNote(),
                            updatedLog.getTimestamp()
                    );
                    if (cellToSave != null) {
                        localDb.cellDao().update(CellEntity.fromDomain(cellToSave, false));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating care log in Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.updateCareLog(zoneId, cellId, updatedLog);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public void deleteCareLog(String zoneId, String cellId, String logId) {
        Zone zone = getZoneById(zoneId);
        Cell targetCell = null;
        if (zone != null) {
            for (Cell cell : zone.getCells()) {
                if (cell.getId().equals(cellId)) {
                    cell.removeCareLog(logId);
                    targetCell = cell;
                    break;
                }
            }
        }

        final Cell cellToSave = targetCell;
        if (localDb != null) {
            diskExecutor.execute(() -> {
                try {
                    localDb.pendingDeletionDao().insert(new PendingDeletionEntity("CARE_LOG", zoneId, cellId, logId, System.currentTimeMillis()));
                    localDb.careLogDao().deleteById(logId);
                    if (cellToSave != null) {
                        localDb.cellDao().update(CellEntity.fromDomain(cellToSave, false));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting care log in Room: ", e);
                }
            });
        }

        if (firestoreService != null) {
            firestoreService.deleteCareLog(zoneId, cellId, logId);
        }

        if (syncManager != null) {
            syncManager.triggerSync();
        }

        notifyDataChanged();
    }

    public int getTotalZonesCount() {
        synchronized (zones) {
            return zones.size();
        }
    }

    public int getTotalPlantsCount() {
        int total = 0;
        synchronized (zones) {
            for (Zone z : zones) {
                total += z.getActivePlantCount();
            }
        }
        return total;
    }

    public int getOverallHealthyPercentage() {
        int healthy = 0;
        int active = 0;
        synchronized (zones) {
            for (Zone z : zones) {
                healthy += z.getHealthyCount();
                active += z.getActivePlantCount();
            }
        }
        if (active == 0) return 100;
        return (int) Math.round((healthy * 100.0) / active);
    }

    public int getTotalIssueCount() {
        int issues = 0;
        synchronized (zones) {
            for (Zone z : zones) {
                issues += z.getWarningCount() + z.getDiseasedCount();
            }
        }
        return issues;
    }
}
