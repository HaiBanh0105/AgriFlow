package com.example.agriflow.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.agriflow.data.firebase.FirestoreService;
import com.example.agriflow.data.local.AppDatabase;
import com.example.agriflow.data.local.dao.CareLogDao;
import com.example.agriflow.data.local.dao.CellDao;
import com.example.agriflow.data.local.dao.PendingDeletionDao;
import com.example.agriflow.data.local.dao.ZoneDao;
import com.example.agriflow.data.local.entity.CareLogEntity;
import com.example.agriflow.data.local.entity.CellEntity;
import com.example.agriflow.data.local.entity.PendingDeletionEntity;
import com.example.agriflow.data.local.entity.ZoneEntity;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.Zone;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SyncManager {
    private static final String TAG = "SyncManager";

    public enum SyncState {
        IDLE,
        SYNCING,
        SYNCED,
        OFFLINE,
        ERROR
    }

    public interface OnSyncStatusListener {
        void onSyncStatusChanged(SyncState state, String message);
    }

    private static volatile SyncManager instance;
    private final Context context;
    private final AppDatabase db;
    private final FirestoreService firestoreService;
    private final ExecutorService executorService;
    private final List<OnSyncStatusListener> listeners = new ArrayList<>();

    private SyncState currentState = SyncState.IDLE;
    private long lastSyncTimestamp = 0L;
    private boolean isNetworkAvailable = false;

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
        this.firestoreService = FirestoreService.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        setupNetworkMonitoring();
    }

    public static SyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager(context);
                }
            }
        }
        return instance;
    }

    public static SyncManager getInstance() {
        return instance;
    }

    public void addListener(OnSyncStatusListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onSyncStatusChanged(currentState, getStatusMessage());
        }
    }

    public void removeListener(OnSyncStatusListener listener) {
        listeners.remove(listener);
    }

    private void updateState(SyncState state, String message) {
        this.currentState = state;
        Log.d(TAG, "SyncState: " + state + " - " + message);
        for (OnSyncStatusListener listener : listeners) {
            listener.onSyncStatusChanged(state, message);
        }
    }

    public SyncState getCurrentState() {
        return currentState;
    }

    public String getStatusMessage() {
        switch (currentState) {
            case SYNCING:
                return "Đang đồng bộ với Cloud...";
            case SYNCED:
                return "Đã đồng bộ Cloud";
            case OFFLINE:
                return "Ngoại tuyến - Dữ liệu lưu trong máy";
            case ERROR:
                return "Lỗi đồng bộ Cloud";
            default:
                return "Sẵn sàng";
        }
    }

    private void setupNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        isNetworkAvailable = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "Network is available, triggering sync...");
                isNetworkAvailable = true;
                triggerSync();
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "Network lost, running offline");
                isNetworkAvailable = false;
                updateState(SyncState.OFFLINE, "Ngoại tuyến - Dữ liệu lưu trong máy");
            }
        });
    }

    public void triggerSync() {
        executorService.execute(this::performSync);
    }

    private synchronized void performSync() {
        if (db == null) return;

        FirebaseFirestore firestore = firestoreService.getDb();
        if (firestore == null || !isNetworkAvailable) {
            updateState(SyncState.OFFLINE, "Ngoại tuyến - Dữ liệu lưu an toàn trong Room");
            return;
        }

        updateState(SyncState.SYNCING, "Đang đồng bộ dữ liệu...");

        try {
            PendingDeletionDao pendingDeletionDao = db.pendingDeletionDao();
            ZoneDao zoneDao = db.zoneDao();
            CellDao cellDao = db.cellDao();
            CareLogDao careLogDao = db.careLogDao();

            // 0. SYNC PENDING DELETIONS
            // IMPORTANT: Only delete from Room queue AFTER Firestore confirms success!
            List<PendingDeletionEntity> pendingDeletions = pendingDeletionDao.getAllPendingDeletions();
            if (pendingDeletions != null && !pendingDeletions.isEmpty()) {
                for (PendingDeletionEntity del : pendingDeletions) {
                    try {
                        Task<Void> deleteTask = null;
                        if ("ZONE".equals(del.getEntityType())) {
                            deleteTask = firestoreService.deleteZoneTask(del.getTargetId());
                        } else if ("CELL".equals(del.getEntityType())) {
                            deleteTask = firestoreService.deleteCellTask(del.getZoneId(), del.getTargetId());
                        } else if ("CARE_LOG".equals(del.getEntityType())) {
                            deleteTask = firestoreService.deleteCareLogTask(del.getZoneId(), del.getCellId(), del.getTargetId());
                        }

                        if (deleteTask != null) {
                            // Synchronously await Firestore completion with timeout on this background thread
                            Tasks.await(deleteTask, 30, TimeUnit.SECONDS);
                            // ONLY remove from pending queue once Firestore has confirmed success!
                            pendingDeletionDao.deleteById(del.getId());
                            Log.d(TAG, "Pending deletion confirmed by Firestore and removed from queue: " + del.getEntityType() + " " + del.getTargetId());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Firestore deletion failed for " + del.getEntityType() + " " + del.getTargetId() + " (will remain in queue for retry): ", e);
                        // Deliberately do not remove from pendingDeletionDao so it is retried next time
                    }
                }
            }

            // 1. UPLOAD: Sync unsynced Zones
            List<ZoneEntity> unsyncedZones = zoneDao.getUnsyncedZones();
            for (ZoneEntity zoneEntity : unsyncedZones) {
                try {
                    if (zoneEntity.isDeleted()) {
                        Task<Void> delTask = firestoreService.deleteZoneTask(zoneEntity.getId());
                        Tasks.await(delTask, 30, TimeUnit.SECONDS);
                        zoneDao.deleteById(zoneEntity.getId());
                    } else {
                        Zone domainZone = zoneEntity.toDomain();
                        Task<Void> saveTask = firestoreService.saveZoneTask(domainZone);
                        Tasks.await(saveTask, 30, TimeUnit.SECONDS);
                        zoneDao.updateSyncStatus(zoneEntity.getId(), true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to sync zone " + zoneEntity.getId() + " (will retry): ", e);
                }
            }

            // 2. UPLOAD: Sync unsynced Cells
            List<CellEntity> unsyncedCells = cellDao.getUnsyncedCells();
            if (!unsyncedCells.isEmpty()) {
                Map<String, List<Cell>> cellsByZone = new HashMap<>();
                List<String> syncedCellIds = new ArrayList<>();

                for (CellEntity entity : unsyncedCells) {
                    Cell cell = entity.toDomain();
                    syncedCellIds.add(cell.getId());
                    if (!cellsByZone.containsKey(cell.getZoneId())) {
                        cellsByZone.put(cell.getZoneId(), new ArrayList<>());
                    }
                    cellsByZone.get(cell.getZoneId()).add(cell);
                }

                try {
                    Task<Void> cellsTask = firestoreService.batchUpdateCellsTask(cellsByZone);
                    Tasks.await(cellsTask, 30, TimeUnit.SECONDS);
                    cellDao.updateBatchSyncStatus(syncedCellIds, true);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to batch update cells (will retry): ", e);
                }
            }

            // 3. UPLOAD: Sync unsynced CareLogs
            List<CareLogEntity> unsyncedLogs = careLogDao.getUnsyncedLogs();
            if (!unsyncedLogs.isEmpty()) {
                List<String> syncedLogIds = new ArrayList<>();
                for (CareLogEntity logEntity : unsyncedLogs) {
                    syncedLogIds.add(logEntity.getId());
                }

                try {
                    Task<Void> logsTask = firestoreService.batchAddCareLogsTask(unsyncedLogs);
                    Tasks.await(logsTask, 30, TimeUnit.SECONDS);
                    careLogDao.updateBatchSyncStatus(syncedLogIds, true);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to batch upload care logs (will retry): ", e);
                }
            }

            lastSyncTimestamp = System.currentTimeMillis();
            updateState(SyncState.SYNCED, "Đã đồng bộ Cloud thành công");
        } catch (Exception e) {
            Log.e(TAG, "Sync error: ", e);
            updateState(SyncState.ERROR, "Lỗi đồng bộ: " + e.getMessage());
        }
    }
}
