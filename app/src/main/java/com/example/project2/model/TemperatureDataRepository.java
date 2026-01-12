package com.example.project2.model;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project2.db.AppDatabase;
import com.example.project2.db.TemperatureHistoryDao;
import com.example.project2.db.TemperatureHistoryEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemperatureDataRepository {
    private static volatile TemperatureDataRepository instance;
    private final MutableLiveData<String> temperature = new MutableLiveData<>();
    private final TemperatureHistoryDao temperatureHistoryDao;
    private final MutableLiveData<List<TemperatureHistoryEntry>> temperatureHistory = new MutableLiveData<>();
    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private TemperatureDataRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        temperatureHistoryDao = db.temperatureHistoryDao();
    }

    public static TemperatureDataRepository getInstance(Application application) {
        if (instance == null) {
            synchronized (TemperatureDataRepository.class) {
                if (instance == null) {
                    instance = new TemperatureDataRepository(application);
                }
            }
        }
        return instance;
    }

    public LiveData<String> getTemperature() {
        return temperature;
    }

    public LiveData<List<TemperatureHistoryEntry>> getTemperatureHistory() {
        return temperatureHistory;
    }

    public void fetchHistoryInRange(long startTime, long endTime) {
        databaseWriteExecutor.execute(() -> {
            List<TemperatureHistoryEntry> history = temperatureHistoryDao.getHistoryInRange(startTime, endTime);
            temperatureHistory.postValue(history);
        });
    }

    public void updateTemperature(String newValue) {
        temperature.postValue(newValue);
        databaseWriteExecutor.execute(() -> {
            temperatureHistoryDao.insert(new TemperatureHistoryEntry(System.currentTimeMillis(), Float.parseFloat(newValue)));
            temperatureHistoryDao.trimHistory();
        });
    }
}