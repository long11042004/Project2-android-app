package com.example.project2.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "temperature_history")
public class TemperatureHistoryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;
    public float temperatureValue;

    public TemperatureHistoryEntry(long timestamp, float temperatureValue) {
        this.timestamp = timestamp;
        this.temperatureValue = temperatureValue;
    }
}