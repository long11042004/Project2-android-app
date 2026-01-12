package com.example.project2.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TemperatureHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TemperatureHistoryEntry entry);

    @Query("SELECT * FROM temperature_history WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    List<TemperatureHistoryEntry> getHistoryInRange(long startTime, long endTime);

    @Query("DELETE FROM temperature_history WHERE id NOT IN (SELECT id FROM temperature_history ORDER BY timestamp DESC LIMIT 500)")
    void trimHistory();
}