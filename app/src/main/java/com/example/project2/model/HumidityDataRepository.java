package com.example.project2.model;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project2.db.AirHumidityHistoryDao;
import com.example.project2.db.AirHumidityHistoryEntry;
import com.example.project2.db.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HumidityDataRepository {
    private static volatile HumidityDataRepository instance;
    private final MutableLiveData<String> airHumidity = new MutableLiveData<>();
    private final AirHumidityHistoryDao airHumidityHistoryDao;
    private final MutableLiveData<List<AirHumidityHistoryEntry>> airHumidityHistory = new MutableLiveData<>();
    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private HumidityDataRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        airHumidityHistoryDao = db.airHumidityHistoryDao();
    }

    public static HumidityDataRepository getInstance(Application application) {
        if (instance == null) {
            synchronized (HumidityDataRepository.class) {
                if (instance == null) {
                    instance = new HumidityDataRepository(application);
                }
            }
        }
        return instance;
    }

    public LiveData<String> getAirHumidity() {
        return airHumidity;
    }

    public LiveData<List<AirHumidityHistoryEntry>> getAirHumidityHistory() {
        return airHumidityHistory;
    }

    public void fetchHistoryInRange(long startTime, long endTime) {
        databaseWriteExecutor.execute(() -> {
            List<AirHumidityHistoryEntry> history = airHumidityHistoryDao.getHistoryInRange(startTime, endTime);
            airHumidityHistory.postValue(history);
        });
    }

    public void updateAirHumidity(String newValue) {
        airHumidity.postValue(newValue);
        databaseWriteExecutor.execute(() -> {
            airHumidityHistoryDao.insert(new AirHumidityHistoryEntry(System.currentTimeMillis(), Float.parseFloat(newValue)));
            airHumidityHistoryDao.trimHistory();
        });
    }
}