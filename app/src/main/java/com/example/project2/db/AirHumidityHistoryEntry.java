package com.example.project2.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "air_humidity_history")
public class AirHumidityHistoryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;
    public float humidityValue;

    public AirHumidityHistoryEntry(long timestamp, float humidityValue) {
        this.timestamp = timestamp;
        this.humidityValue = humidityValue;
    }
}