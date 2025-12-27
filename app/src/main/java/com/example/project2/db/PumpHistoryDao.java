package com.example.project2.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PumpHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PumpHistoryEntry entry);

    // Lấy lịch sử để tính toán thời gian chạy
    @Query("SELECT * FROM pump_history ORDER BY timestamp ASC")
    LiveData<List<PumpHistoryEntry>> getAllHistory();

    // Xóa dữ liệu cũ quá 7 ngày (hoặc giới hạn số lượng)
    @Query("DELETE FROM pump_history WHERE timestamp < :threshold")
    void deleteOldHistory(long threshold);
}