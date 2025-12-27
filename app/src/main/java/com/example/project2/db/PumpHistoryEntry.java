package com.example.project2.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pump_history")
public class PumpHistoryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;
    public int status; // 1 = ON, 0 = OFF

    public PumpHistoryEntry(long timestamp, int status) {
        this.timestamp = timestamp;
        this.status = status;
    }
}