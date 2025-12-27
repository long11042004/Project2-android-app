package com.example.project2.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        MoistureHistoryEntry.class,
        TemperatureHistoryEntry.class,
        AirHumidityHistoryEntry.class,
        PumpHistoryEntry.class
}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MoistureHistoryDao moistureHistoryDao();
    public abstract TemperatureHistoryDao temperatureHistoryDao();
    public abstract AirHumidityHistoryDao airHumidityHistoryDao();
    public abstract PumpHistoryDao pumpHistoryDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "app_database")
                            .fallbackToDestructiveMigration() // Xóa dữ liệu cũ nếu thay đổi version
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}