package com.fivenightsatajisland.aticaobeta.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ScanHistory.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ScanHistoryDao scanHistoryDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "aticao_database")
                            .allowMainThreadQueries() // Simplification for this app
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}