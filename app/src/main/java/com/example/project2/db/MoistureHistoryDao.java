package com.example.project2.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MoistureHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MoistureHistoryEntry entry);

    @Query("SELECT * FROM moisture_history ORDER BY timestamp ASC")
    LiveData<List<MoistureHistoryEntry>> getAllHistory();

    @Query("DELETE FROM moisture_history WHERE id NOT IN (SELECT id FROM moisture_history ORDER BY timestamp DESC LIMIT 500)")
    void trimHistory();
}