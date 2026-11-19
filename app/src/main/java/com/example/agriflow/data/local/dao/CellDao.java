package com.example.agriflow.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.agriflow.data.local.entity.CellEntity;

import java.util.List;

@Dao
public interface CellDao {

    @Query("SELECT * FROM cells WHERE zoneId = :zoneId ORDER BY rowIndex, colIndex")
    List<CellEntity> getCellsByZone(String zoneId);

    @Query("SELECT * FROM cells WHERE id = :id LIMIT 1")
    CellEntity getCellById(String id);

    @Query("SELECT * FROM cells WHERE isSynced = 0")
    List<CellEntity> getUnsyncedCells();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CellEntity cell);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CellEntity> cells);

    @Update
    void update(CellEntity cell);

    @Update
    void updateAll(List<CellEntity> cells);

    @Query("UPDATE cells SET isSynced = :synced WHERE id = :id")
    void updateSyncStatus(String id, boolean synced);

    @Query("UPDATE cells SET isSynced = :synced WHERE id IN (:ids)")
    void updateBatchSyncStatus(List<String> ids, boolean synced);

    @Query("DELETE FROM cells WHERE zoneId = :zoneId")
    void deleteByZone(String zoneId);
}
