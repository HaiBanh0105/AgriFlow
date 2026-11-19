package com.example.agriflow.data.firebase;

import android.util.Log;

import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.CareLog;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.model.Zone;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
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
     * Create a new zone and batch write all its initial cells
     */
    public void saveZone(Zone zone) {
        if (db == null) return;
        try {
            DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zone.getId());

            // 1. Save zone document
            zoneRef.set(zone.toMap())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Zone saved: " + zone.getName()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving zone: ", e));

            // 2. Batch write all cells
            List<Cell> cells = zone.getCells();
            if (cells == null || cells.isEmpty()) return;

            int batchSize = 450;
            for (int i = 0; i < cells.size(); i += batchSize) {
                WriteBatch batch = db.batch();
                int end = Math.min(i + batchSize, cells.size());
                for (int j = i; j < end; j++) {
                    Cell cell = cells.get(j);
                    DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
                    batch.set(cellRef, cell.toMap());
                }
                batch.commit()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Batch cells written successfully"))
                        .addOnFailureListener(e -> Log.e(TAG, "Error writing batch cells: ", e));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving zone to Firestore: ", e);
        }
    }

    /**
     * Update single cell
     */
    public void updateCell(String zoneId, Cell cell) {
        if (db == null) return;
        try {
            DocumentReference cellRef = db.collection(COLLECTION_ZONES)
                    .document(zoneId)
                    .collection(SUBCOLLECTION_CELLS)
                    .document(cell.getId());

            cellRef.set(cell.toMap())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Cell updated: " + cell.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating cell: ", e));
        } catch (Exception e) {
            Log.e(TAG, "Error updating cell: ", e);
        }
    }

    /**
     * Bulk update cell status
     */
    public void bulkUpdateStatus(String zoneId, List<Cell> targetCells, CellStatus newStatus) {
        if (db == null || targetCells == null || targetCells.isEmpty()) return;

        try {
            DocumentReference zoneRef = db.collection(COLLECTION_ZONES).document(zoneId);
            WriteBatch batch = db.batch();

            for (Cell cell : targetCells) {
                DocumentReference cellRef = zoneRef.collection(SUBCOLLECTION_CELLS).document(cell.getId());
                batch.update(cellRef, "status", newStatus.name());
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Bulk status updated successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error in bulk status update: ", e));
        } catch (Exception e) {
            Log.e(TAG, "Error in bulkUpdateStatus: ", e);
        }
    }

    /**
     * Bulk perform care action (Water, Fertilize, etc.)
     */
    public void bulkPerformAction(String zoneId, List<Cell> targetCells, ActionType actionType, String note) {
        if (db == null || targetCells == null || targetCells.isEmpty()) return;

        try {
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
                    batch.update(cellRef, "status", CellStatus.EMPTY.name());
                }

                // Create CareLog in subcollection
                String logId = UUID.randomUUID().toString();
                CareLog log = new CareLog(logId, cell.getId(), actionType, note, now);
                DocumentReference logRef = cellRef.collection(SUBCOLLECTION_CARE_LOGS).document(logId);
                batch.set(logRef, log.toMap());
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Bulk action performed and committed to Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error performing bulk action: ", e));
        } catch (Exception e) {
            Log.e(TAG, "Error in bulkPerformAction: ", e);
        }
    }

    /**
     * Add single care log
     */
    public void addCareLog(String zoneId, String cellId, CareLog log) {
        if (db == null) return;
        try {
            db.collection(COLLECTION_ZONES)
                    .document(zoneId)
                    .collection(SUBCOLLECTION_CELLS)
                    .document(cellId)
                    .collection(SUBCOLLECTION_CARE_LOGS)
                    .document(log.getId())
                    .set(log.toMap())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "CareLog added to Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding CareLog: ", e));
        } catch (Exception e) {
            Log.e(TAG, "Error adding CareLog: ", e);
        }
    }

    /**
     * Delete zone
     */
    public void deleteZone(String zoneId) {
        if (db == null) return;
        try {
            db.collection(COLLECTION_ZONES)
                    .document(zoneId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Zone deleted: " + zoneId))
                    .addOnFailureListener(e -> Log.e(TAG, "Error deleting zone: ", e));
        } catch (Exception e) {
            Log.e(TAG, "Error deleting zone: ", e);
        }
    }
}
