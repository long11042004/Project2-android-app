package com.example.project2.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "moisture_history")
public class MoistureHistoryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;
    public float moistureValue;

    public MoistureHistoryEntry(long timestamp, float moistureValue) {
        this.timestamp = timestamp;
        this.moistureValue = moistureValue;
    }
}