package com.example.agriflow.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.agriflow.data.local.entity.ZoneEntity;

import java.util.List;

@Dao
public interface ZoneDao {

    @Query("SELECT * FROM zones WHERE isDeleted = 0 ORDER BY createdAt DESC")
    List<ZoneEntity> getAllZones();

    @Query("SELECT * FROM zones WHERE id = :id AND isDeleted = 0 LIMIT 1")
    ZoneEntity getZoneById(String id);

    @Query("SELECT * FROM zones WHERE isSynced = 0")
    List<ZoneEntity> getUnsyncedZones();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ZoneEntity zone);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ZoneEntity> zones);

    @Update
    void update(ZoneEntity zone);

    @Query("UPDATE zones SET isSynced = :synced WHERE id = :id")
    void updateSyncStatus(String id, boolean synced);

    @Query("UPDATE zones SET isDeleted = 1, isSynced = 0, updatedAt = :now WHERE id = :id")
    void softDelete(String id, long now);

    @Query("DELETE FROM zones WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM zones")
    void deleteAll();
}
