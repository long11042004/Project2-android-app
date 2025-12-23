package com.example.project2.model;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project2.db.AppDatabase;
import com.example.project2.db.MoistureHistoryDao;
import com.example.project2.db.MoistureHistoryEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoistureDataRepository {
    private static volatile MoistureDataRepository instance;
    private final MutableLiveData<String> soilMoisture = new MutableLiveData<>();
    private final MoistureHistoryDao moistureHistoryDao;
    private final LiveData<List<MoistureHistoryEntry>> allHistory;
    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private MoistureDataRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        moistureHistoryDao = db.moistureHistoryDao();
        allHistory = moistureHistoryDao.getAllHistory();
    }

    public static MoistureDataRepository getInstance(Application application) {
        if (instance == null) {
            synchronized (MoistureDataRepository.class) {
                if (instance == null) {
                    instance = new MoistureDataRepository(application);
                }
            }
        }
        return instance;
    }

    public LiveData<String> getSoilMoisture() {
        return soilMoisture;
    }
    public LiveData<List<MoistureHistoryEntry>> getFullHistory() { return allHistory; }

    public void updateSoilMoisture(String newValue) {
        soilMoisture.postValue(newValue);
        databaseWriteExecutor.execute(() -> {
            moistureHistoryDao.insert(new MoistureHistoryEntry(System.currentTimeMillis(), Float.parseFloat(newValue)));
            moistureHistoryDao.trimHistory(); // Giữ cho DB không quá lớn
        });
    }
}