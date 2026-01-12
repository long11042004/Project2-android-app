package com.example.project2.model;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.project2.db.AppDatabase;
import com.example.project2.db.PumpHistoryDao;
import com.example.project2.db.PumpHistoryEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PumpDataRepository {
    private static volatile PumpDataRepository instance;
    private final PumpHistoryDao pumpHistoryDao;
    private final LiveData<List<PumpHistoryEntry>> pumpHistory;
    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private PumpDataRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        pumpHistoryDao = db.pumpHistoryDao();
        pumpHistory = pumpHistoryDao.getAllHistory();
    }

    public static PumpDataRepository getInstance(Application application) {
        if (instance == null) {
            synchronized (PumpDataRepository.class) {
                if (instance == null) {
                    instance = new PumpDataRepository(application);
                }
            }
        }
        return instance;
    }

    public LiveData<List<PumpHistoryEntry>> getPumpHistory() {
        return pumpHistory;
    }

    public void updatePumpStatus(String statusMessage) {
        // Phân tích trạng thái: 1 = ON, 0 = OFF
        boolean isPumpOn = statusMessage.contains("DANG BAT") || statusMessage.contains("DANG TUOI");
        int status = isPumpOn ? 1 : 0;

        databaseWriteExecutor.execute(() -> {
            pumpHistoryDao.insert(new PumpHistoryEntry(System.currentTimeMillis(), status));
            // Đã vô hiệu hóa việc tự động xóa dữ liệu cũ hơn 7 ngày.
            // pumpHistoryDao.deleteOldHistory(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L);
        });
    }
}