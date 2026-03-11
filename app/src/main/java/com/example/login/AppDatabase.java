package com.example.login;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// INCREMENT THIS VERSION. If it was 1, make it 2. If it was 2, make it 3.
@Database(entities = {Resume.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ResumeDao resumeDao();
    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "resume_database")
                            // THIS LINE IS CRITICAL: It deletes the old broken database
                            // and creates a new one automatically when the version changes.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}