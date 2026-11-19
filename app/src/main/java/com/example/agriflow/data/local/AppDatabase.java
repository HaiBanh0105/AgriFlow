package com.example.agriflow.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.agriflow.data.local.dao.CareLogDao;
import com.example.agriflow.data.local.dao.CellDao;
import com.example.agriflow.data.local.dao.PendingDeletionDao;
import com.example.agriflow.data.local.dao.ZoneDao;
import com.example.agriflow.data.local.entity.CareLogEntity;
import com.example.agriflow.data.local.entity.CellEntity;
import com.example.agriflow.data.local.entity.PendingDeletionEntity;
import com.example.agriflow.data.local.entity.ZoneEntity;

@Database(
        entities = {ZoneEntity.class, CellEntity.class, CareLogEntity.class, PendingDeletionEntity.class},
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "farmgrid_local.db";
    private static volatile AppDatabase instance;

    public abstract ZoneDao zoneDao();
    public abstract CellDao cellDao();
    public abstract CareLogDao careLogDao();
    public abstract PendingDeletionDao pendingDeletionDao();

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `pending_deletions` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`entityType` TEXT, " +
                    "`zoneId` TEXT, " +
                    "`cellId` TEXT, " +
                    "`targetId` TEXT, " +
                    "`createdAt` INTEGER NOT NULL)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }

    public static AppDatabase getInstance() {
        return instance;
    }
}
