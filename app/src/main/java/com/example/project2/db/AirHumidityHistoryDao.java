package com.example.project2.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AirHumidityHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AirHumidityHistoryEntry entry);

    @Query("SELECT * FROM air_humidity_history ORDER BY timestamp ASC")
    LiveData<List<AirHumidityHistoryEntry>> getAllHistory();

    @Query("DELETE FROM air_humidity_history WHERE id NOT IN (SELECT id FROM air_humidity_history ORDER BY timestamp DESC LIMIT 500)")
    void trimHistory();
}