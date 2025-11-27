package com.example.sky;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static String DB_PATH = "";
    private static final String DB_NAME = "skyDB.db";
    private final Context mContext;
    private SQLiteDatabase mDatabase;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.mContext = context;
        // Путь к базе данных в системном хранилище Android
        DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
    }

    // Метод для создания базы данных: копирует ее из assets, если она не существует
    public void createDatabase() throws IOException {
        // Проверяем, существует ли уже база данных
        boolean dbExist = checkDatabase();

        if (!dbExist) {
            // Если не существует, вызываем этот метод (он создает пустую БД по пути DB_PATH)
            this.getReadableDatabase();
            try {
                // Копируем файл из assets
                copyDatabase();
                Log.e(TAG, "База данных успешно скопирована из assets.");
            } catch (IOException e) {
                throw new Error("Ошибка копирования базы данных");
            }
        }
    }

    // Проверка существования файла базы данных по указанному пути
    private boolean checkDatabase() {
        File dbFile = new File(DB_PATH + DB_NAME);
        return dbFile.exists();
    }

    // Копирование файла базы данных из папки assets
    private void copyDatabase() throws IOException {
        // Открываем локальный файл БД как поток ввода
        InputStream myInput = mContext.getAssets().open(DB_NAME);

        // Путь назначения (системное хранилище)
        String outFileName = DB_PATH + DB_NAME;

        // Открываем пустой файл как поток вывода
        OutputStream myOutput = new FileOutputStream(outFileName);

        // Перемещаем байты из входного файла в выходной
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // Закрываем потоки
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    // Открытие базы данных
    public void openDatabase() throws SQLException {
        String myPath = DB_PATH + DB_NAME;
        mDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public synchronized void close() {
        if (mDatabase != null) {
            mDatabase.close();
        }
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Здесь ничего не делаем, так как мы копируем уже готовую БД
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Обработка обновления версии БД при необходимости
    }

    // Геттер для доступа к экземпляру базы данных
    public SQLiteDatabase getDatabase() {
        return mDatabase;
    }


}
