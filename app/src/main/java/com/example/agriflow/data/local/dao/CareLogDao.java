package com.example.agriflow.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.agriflow.data.local.entity.CareLogEntity;

import java.util.List;

@Dao
public interface CareLogDao {

    @Query("SELECT * FROM care_logs WHERE cellId = :cellId ORDER BY timestamp DESC")
    List<CareLogEntity> getLogsByCell(String cellId);

    @Query("SELECT * FROM care_logs WHERE isSynced = 0")
    List<CareLogEntity> getUnsyncedLogs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CareLogEntity log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CareLogEntity> logs);

    @androidx.room.Update
    void update(CareLogEntity log);

    @Query("UPDATE care_logs SET actionType = :actionType, note = :note, timestamp = :timestamp, isSynced = 0 WHERE id = :id")
    void updateLog(String id, String actionType, String note, long timestamp);

    @Query("DELETE FROM care_logs WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE care_logs SET isSynced = :synced WHERE id = :id")
    void updateSyncStatus(String id, boolean synced);

    @Query("UPDATE care_logs SET isSynced = :synced WHERE id IN (:ids)")
    void updateBatchSyncStatus(List<String> ids, boolean synced);

    @Query("DELETE FROM care_logs WHERE cellId = :cellId")
    void deleteByCell(String cellId);

    @Query("DELETE FROM care_logs WHERE zoneId = :zoneId")
    void deleteByZone(String zoneId);
}
