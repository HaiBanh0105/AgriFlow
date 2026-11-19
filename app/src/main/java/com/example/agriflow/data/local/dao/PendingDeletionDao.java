package com.example.agriflow.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.agriflow.data.local.entity.PendingDeletionEntity;

import java.util.List;

@Dao
public interface PendingDeletionDao {

    @Query("SELECT * FROM pending_deletions ORDER BY createdAt ASC")
    List<PendingDeletionEntity> getAllPendingDeletions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PendingDeletionEntity entity);

    @Query("DELETE FROM pending_deletions WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM pending_deletions WHERE targetId = :targetId")
    void deleteByTargetId(String targetId);

    @Query("DELETE FROM pending_deletions")
    void deleteAll();
}
