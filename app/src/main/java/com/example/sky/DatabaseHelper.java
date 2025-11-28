package com.example.sky;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static String DB_PATH = "";
    private static final String DB_NAME = "MyDB.db";
    private SQLiteDatabase mDatabase;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
    }

    // Этот метод теперь просто открывает БД, которая УЖЕ должна быть скачана
    public void openDatabase() throws SQLException {
        String myPath = DB_PATH + DB_NAME;
        File dbFile = new File(myPath);
        if (dbFile.exists()) {
            mDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
            Log.d(TAG, "Database opened successfully.");
        } else {
            Log.e(TAG, "Database file not found at path: " + myPath);
            throw new SQLException("Database file does not exist.");
        }
    }

    // Метод-заглушка, так как мы не создаем БД с нуля
    @Override
    public void onCreate(SQLiteDatabase db) {}

    // Метод-заглушка
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public synchronized void close() {
        if (mDatabase != null) {
            mDatabase.close();
        }
        super.close();
    }

    public SQLiteDatabase getDatabase() {
        return mDatabase;
    }

    // Проверяем, был ли файл уже скачан.
    public boolean isDatabaseDownloaded() {
        File dbFile = new File(DB_PATH + DB_NAME);
        return dbFile.exists() && dbFile.length() > 0;
    }
}
