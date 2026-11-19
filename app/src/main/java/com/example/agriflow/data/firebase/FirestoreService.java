package com.example.agriflow.data.firebase;

import android.util.Log;

import com.example.agriflow.data.local.entity.CareLogEntity;
import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.CareLog;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.model.Zone;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirestoreService {
    private static final String TAG = "FirestoreService";
    private static final String COLLECTION_ZONES = "zones";
    private static final String SUBCOLLECTION_CELLS = "cells";
    private static final String SUBCOLLECTION_CARE_LOGS = "careLogs";

    private static FirestoreService instance;
    private final FirebaseFirestore db;

    public interface OnZonesLoadedListener {
        void onZonesLoaded(List<Zone> zones);
        void onError(Exception e);
    }

    public interface OnCellsLoadedListener {
        void onCellsLoaded(List<Cell> cells);
        void onError(Exception e);
    }

    private FirestoreService() {
        FirebaseFirestore firestoreInstance = null;
        try {
            firestoreInstance = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            firestoreInstance.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore offline persistence enabled successfully");
        } catch (Exception e) {
            Log.w(TAG, "Firestore initialization warning (running in mock/test mode): " + e.getMessage());
        }
        db = firestoreInstance;
    }

    public static synchronized FirestoreService getInstance() {
        if (instance == null) {
            instance = new FirestoreService();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Listen to real-time updates for zones
     */
    public ListenerRegistration listenToZones(OnZonesLoadedListener listener) {
        if (db == null) return null;
        try {
            return db.collection(COLLECTION_ZONES)
                    .orderBy("createdAt")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Listen failed: ", e);
                            if (listener != null) listener.onError(e);
                            return;
                        }

                        if (snapshots != null) {
                            List<Zone> zones = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Zone zone = Zone.fromMap(doc.getData());
                                if (zone != null) {
                                    zones.add(zone);
                                }
                            }
                            if (listener != null) {
                                listener.onZonesLoaded(zones);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error attaching listenToZones: ", e);
            return null;
        }
    }

    /**
     * Load cells for a specific zone from Firestore
     */
    public ListenerRegistration listenToZoneCells(String zoneId, OnCellsLoadedListener listener) {
        if (db == null) return null;
        try {
            return db.collection(COLLECTION_ZONES)
                    .document(zoneId)
                    .collection(SUBCOLLECTION_CELLS)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Cells listen failed: ", e);
                            if (listener != null) listener.onError(e);
                            return;
                        }

                        if (snapshots != null) {
                            List<Cell> cells = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Cell cell = Cell.fromMap(doc.getData());
                                if (cell != null) {
                                    cells.add(cell);
                                }
                            }
                            if (listener != null) {
                                listener.onCellsLoaded(cells);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error attaching listenToZoneCells: ", e);
            return null;
        }
    }

    /**
     * Save zone task: writes zone document and all cells
     */
    public Task<Void> saveZoneTask(Zone zone) {
        if (db == null || zone == null) {
            return Tasks.forResult(null);
        }
        DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zone.getId());

        return zoneRef.set(zone.toMap())
                .continueWithTask(zoneTask -> {
                    if (!zoneTask.isSuccessful()) {
                        throw zoneTask.getException() != null ?
                                zoneTask.getException() : new Exception("Failed to save zone doc");
                    }
                    List<Cell> cells = zone.getCells();
                    if (cells == null || cells.isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    List<Task<Void>> batchTasks = new ArrayList<>();
                    int batchSize = 450;
                    for (int i = 0; i < cells.size(); i += batchSize) {
                        WriteBatch batch = db.batch();
                        int end = Math.min(i + batchSize, cells.size());
                        for (int j = i; j < end; j++) {
                            Cell cell = cells.get(j);
                            DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
                            batch.set(cellRef, cell.toMap());
                        }
                        batchTasks.add(batch.commit());
                    }
                    return Tasks.whenAll(batchTasks);
                });
    }

    public void saveZone(Zone zone) {
        saveZoneTask(zone)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Zone and cells saved to Firestore: " + (zone != null ? zone.getName() : "")))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving zone: ", e));
    }

    /**
     * Update zone metadata (name, description, default crop, cycle, dimensions)
     */
    public Task<Void> updateZoneInfoTask(String zoneId, String name, String description, String defaultCrop, int harvestCycleDays, int rows, int cols) {
        if (db == null || zoneId == null) {
            return Tasks.forResult(null);
        }
        DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("defaultCrop", defaultCrop);
        updates.put("harvestCycleDays", harvestCycleDays);
        updates.put("rows", rows);
        updates.put("cols", cols);
        updates.put("updatedAt", System.currentTimeMillis());

        return zoneRef.update(updates);
    }

    public void updateZoneInfo(String zoneId, String name, String description, String defaultCrop, int harvestCycleDays, int rows, int cols) {
        updateZoneInfoTask(zoneId, name, description, defaultCrop, harvestCycleDays, rows, cols)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Zone info updated in Firestore: " + zoneId))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating zone info in Firestore: ", e));
    }

    /**
     * Task to delete a single cell and all careLogs subcollection documents beneath it
     */
    public Task<Void> deleteCellTask(String zoneId, String cellId) {
        if (db == null || zoneId == null || cellId == null) {
            return Tasks.forResult(null);
        }
        DocumentReference cellRef = db.collection(COLLECTION_ZONES)
                .document(zoneId)
                .collection(SUBCOLLECTION_CELLS)
                .document(cellId);

        // 1. Fetch careLogs under this cell
        return cellRef.collection(SUBCOLLECTION_CARE_LOGS).get()
                .continueWithTask(logsTask -> {
                    WriteBatch batch = db.batch();
                    if (logsTask.isSuccessful() && logsTask.getResult() != null) {
                        for (QueryDocumentSnapshot logDoc : logsTask.getResult()) {
                            batch.delete(logDoc.getReference());
                        }
                    }
                    // 2. Delete cell document itself
                    batch.delete(cellRef);
                    return batch.commit();
                });
    }

    public void deleteCell(String zoneId, String cellId) {
        deleteCellTask(zoneId, cellId)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cell and all its careLogs deleted: " + cellId))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting cell: ", e));
    }

    /**
     * Task to delete list of cells and all their careLogs
     */
    public Task<Void> deleteCellsTask(String zoneId, List<String> cellIds) {
        if (db == null || cellIds == null || cellIds.isEmpty()) {
            return Tasks.forResult(null);
        }
        List<Task<Void>> tasks = new ArrayList<>();
        for (String cellId : cellIds) {
            tasks.add(deleteCellTask(zoneId, cellId));
        }
        return Tasks.whenAll(tasks);
    }

    public void deleteCells(String zoneId, List<String> cellIds) {
        deleteCellsTask(zoneId, cellIds)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Batch cells and their careLogs deleted from Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting batch cells: ", e));
    }

    /**
     * Task to recursively delete a zone:
     * 1. Query all cells under zones/{zoneId}/cells
     * 2. For each cell, query and delete all careLogs under zones/{zoneId}/cells/{cellId}/careLogs
     * 3. Delete each cell document
     * 4. Delete the zone document zones/{zoneId}
     */
    public Task<Void> deleteZoneTask(String zoneId) {
        if (db == null || zoneId == null) {
            return Tasks.forResult(null);
        }
        DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);

        return zoneRef.collection(SUBCOLLECTION_CELLS).get()
                .continueWithTask(cellsTask -> {
                    if (!cellsTask.isSuccessful()) {
                        throw cellsTask.getException() != null ?
                                cellsTask.getException() : new Exception("Failed to query cells for zone deletion");
                    }
                    QuerySnapshot cellsSnapshot = cellsTask.getResult();
                    List<Task<Void>> cellDeleteTasks = new ArrayList<>();
                    if (cellsSnapshot != null && !cellsSnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot cellDoc : cellsSnapshot) {
                            // Recursively delete this cell and all its careLogs
                            cellDeleteTasks.add(deleteCellTask(zoneId, cellDoc.getId()));
                        }
                    }
                    return Tasks.whenAll(cellDeleteTasks)
                            .continueWithTask(allCellsDeletedTask -> {
                                if (!allCellsDeletedTask.isSuccessful()) {
                                    throw allCellsDeletedTask.getException() != null ?
                                            allCellsDeletedTask.getException() : new Exception("Failed to delete cells under zone");
                                }
                                // Finally delete the zone document itself
                                return zoneRef.delete();
                            });
                });
    }

    public void deleteZone(String zoneId) {
        deleteZoneTask(zoneId)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Zone and all subcollections recursively deleted from Firestore: " + zoneId))
                .addOnFailureListener(e -> Log.e(TAG, "Error in recursive zone deletion: ", e));
    }

    /**
     * Update single cell
     */
    public Task<Void> updateCellTask(String zoneId, Cell cell) {
        if (db == null || cell == null) {
            return Tasks.forResult(null);
        }
        DocumentReference cellRef = db.collection(COLLECTION_ZONES)
                .document(zoneId)
                .collection(SUBCOLLECTION_CELLS)
                .document(cell.getId());
        return cellRef.set(cell.toMap());
    }

    public void updateCell(String zoneId, Cell cell) {
        updateCellTask(zoneId, cell)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cell updated: " + cell.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating cell: ", e));
    }

    /**
     * Batch update multiple cells grouped by zoneId
     */
    public Task<Void> batchUpdateCellsTask(Map<String, List<Cell>> cellsByZone) {
        if (db == null || cellsByZone == null || cellsByZone.isEmpty()) {
            return Tasks.forResult(null);
        }
        List<Task<Void>> batchTasks = new ArrayList<>();
        for (Map.Entry<String, List<Cell>> entry : cellsByZone.entrySet()) {
            String zoneId = entry.getKey();
            List<Cell> cells = entry.getValue();
            DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);
            int batchSize = 450;
            for (int i = 0; i < cells.size(); i += batchSize) {
                WriteBatch batch = db.batch();
                int end = Math.min(i + batchSize, cells.size());
                for (int j = i; j < end; j++) {
                    Cell cell = cells.get(j);
                    DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
                    batch.set(cellRef, cell.toMap());
                }
                batchTasks.add(batch.commit());
            }
        }
        return Tasks.whenAll(batchTasks);
    }

    /**
     * Bulk update cell status
     */
    public Task<Void> bulkUpdateStatusTask(String zoneId, List<Cell> targetCells, CellStatus newStatus) {
        if (db == null || targetCells == null || targetCells.isEmpty()) {
            return Tasks.forResult(null);
        }
        DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);
        WriteBatch batch = db.batch();

        for (Cell cell : targetCells) {
            DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
            batch.update(cellRef, "status", newStatus.name());
        }
        return batch.commit();
    }

    public void bulkUpdateStatus(String zoneId, List<Cell> targetCells, CellStatus newStatus) {
        bulkUpdateStatusTask(zoneId, targetCells, newStatus)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Bulk status updated successfully in Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error in bulk status update: ", e));
    }

    /**
     * Bulk perform care action (Water, Fertilize, Pesticide, etc.)
     */
    public Task<Void> bulkPerformActionTask(String zoneId, List<Cell> targetCells, ActionType actionType, String note) {
        if (db == null || targetCells == null || targetCells.isEmpty()) {
            return Tasks.forResult(null);
        }
        long now = System.currentTimeMillis();
        DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);
        WriteBatch batch = db.batch();

        for (Cell cell : targetCells) {
            DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
            if (actionType == ActionType.WATER) {
                batch.update(cellRef, "lastWatered", now);
                if (cell.getStatus() == CellStatus.WARNING) {
                    batch.update(cellRef, "status", CellStatus.HEALTHY.name());
                }
            } else if (actionType == ActionType.FERTILIZE) {
                batch.update(cellRef, "lastFertilized", now);
            } else if (actionType == ActionType.HARVEST) {
                batch.update(cellRef, "lastHarvested", now);
                batch.update(cellRef, "status", CellStatus.EMPTY.name());
                batch.update(cellRef, "plantType", "");
            } else if (actionType == ActionType.PESTICIDE) {
                if (cell.getStatus() == CellStatus.DISEASED) {
                    batch.update(cellRef, "status", CellStatus.WARNING.name());
                }
            }

            // Create CareLog in subcollection
            String logId = UUID.randomUUID().toString();
            CareLog log = new CareLog(logId, cell.getId(), actionType, note, now);
            DocumentReference logRef = cellRef.collection(SUBCOLLECTION_CARE_LOGS).document(logId);
            batch.set(logRef, log.toMap());
        }
        return batch.commit();
    }

    public void bulkPerformAction(String zoneId, List<Cell> targetCells, ActionType actionType, String note) {
        bulkPerformActionTask(zoneId, targetCells, actionType, note)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Bulk action performed and committed to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error performing bulk action: ", e));
    }

    /**
     * Bulk plant crops into target cells
     */
    public Task<Void> bulkPlantCropsTask(String zoneId, List<Cell> targetCells, String cropName, String note) {
        if (db == null || targetCells == null || targetCells.isEmpty()) {
            return Tasks.forResult(null);
        }
        long now = System.currentTimeMillis();
        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date(now));
        DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);
        WriteBatch batch = db.batch();

        for (Cell cell : targetCells) {
            DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
            batch.update(cellRef, "status", CellStatus.HEALTHY.name());
            batch.update(cellRef, "plantType", cropName);
            batch.update(cellRef, "plantedDate", dateStr);

            String logId = UUID.randomUUID().toString();
            CareLog log = new CareLog(logId, cell.getId(), ActionType.PLANT, note != null && !note.isEmpty() ? note : "Gieo trồng giống " + cropName, now);
            DocumentReference logRef = cellRef.collection(SUBCOLLECTION_CARE_LOGS).document(logId);
            batch.set(logRef, log.toMap());
        }
        return batch.commit();
    }

    public void bulkPlantCrops(String zoneId, List<Cell> targetCells, String cropName, String note) {
        bulkPlantCropsTask(zoneId, targetCells, cropName, note)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Bulk plant committed to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error committing bulk plant to Firestore: ", e));
    }

    /**
     * Bulk harvest target cells
     */
    public Task<Void> bulkHarvestTask(String zoneId, List<Cell> targetCells, String yieldNote, boolean keepTree) {
        if (db == null || targetCells == null || targetCells.isEmpty()) {
            return Tasks.forResult(null);
        }
        long now = System.currentTimeMillis();
        DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);
        WriteBatch batch = db.batch();

        for (Cell cell : targetCells) {
            DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
            batch.update(cellRef, "lastHarvested", now);
            if (keepTree) {
                if (cell.getStatus() == CellStatus.WARNING) {
                    batch.update(cellRef, "status", CellStatus.HEALTHY.name());
                }
            } else {
                batch.update(cellRef, "status", CellStatus.EMPTY.name());
                batch.update(cellRef, "plantType", "");
            }

            String logId = UUID.randomUUID().toString();
            CareLog log = new CareLog(logId, cell.getId(), ActionType.HARVEST, yieldNote, now);
            DocumentReference logRef = cellRef.collection(SUBCOLLECTION_CARE_LOGS).document(logId);
            batch.set(logRef, log.toMap());
        }
        return batch.commit();
    }

    public void bulkHarvest(String zoneId, List<Cell> targetCells, String yieldNote, boolean keepTree) {
        bulkHarvestTask(zoneId, targetCells, yieldNote, keepTree)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Bulk harvest committed to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error committing bulk harvest to Firestore: ", e));
    }

    /**
     * Add single care log
     */
    public Task<Void> addCareLogTask(String zoneId, String cellId, CareLog log) {
        if (db == null || log == null) {
            return Tasks.forResult(null);
        }
        return db.collection(COLLECTION_ZONES)
                .document(zoneId)
                .collection(SUBCOLLECTION_CELLS)
                .document(cellId)
                .collection(SUBCOLLECTION_CARE_LOGS)
                .document(log.getId())
                .set(log.toMap());
    }

    public void addCareLog(String zoneId, String cellId, CareLog log) {
        addCareLogTask(zoneId, cellId, log)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "CareLog synced to Firestore: " + (log != null ? log.getId() : "")))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing CareLog: ", e));
    }

    /**
     * Batch save care logs
     */
    public Task<Void> batchAddCareLogsTask(List<CareLogEntity> logEntities) {
        if (db == null || logEntities == null || logEntities.isEmpty()) {
            return Tasks.forResult(null);
        }
        List<Task<Void>> tasks = new ArrayList<>();
        for (CareLogEntity entity : logEntities) {
            tasks.add(addCareLogTask(entity.getZoneId(), entity.getCellId(), entity.toDomain()));
        }
        return Tasks.whenAll(tasks);
    }

    /**
     * Update single care log
     */
    public Task<Void> updateCareLogTask(String zoneId, String cellId, CareLog log) {
        return addCareLogTask(zoneId, cellId, log);
    }

    public void updateCareLog(String zoneId, String cellId, CareLog log) {
        updateCareLogTask(zoneId, cellId, log)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "CareLog updated in Firestore: " + (log != null ? log.getId() : "")))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating CareLog: ", e));
    }

    /**
     * Task to delete a single care log
     */
    public Task<Void> deleteCareLogTask(String zoneId, String cellId, String logId) {
        if (db == null || zoneId == null || cellId == null || logId == null) {
            return Tasks.forResult(null);
        }
        return db.collection(COLLECTION_ZONES)
                .document(zoneId)
                .collection(SUBCOLLECTION_CELLS)
                .document(cellId)
                .collection(SUBCOLLECTION_CARE_LOGS)
                .document(logId)
                .delete();
    }

    public void deleteCareLog(String zoneId, String cellId, String logId) {
        deleteCareLogTask(zoneId, cellId, logId)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "CareLog deleted from Firestore: " + logId))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting CareLog from Firestore: ", e));
    }
}
