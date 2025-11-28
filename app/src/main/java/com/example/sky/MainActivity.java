package com.example.sky;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    // Используем предоставленный вами File ID для формирования прямой ссылки
    private static final String FILE_ID = "1TKyECifFSflpaEyHY044oiQjQi9_zb9P";
    private static final String TAG = "MainActivity";
    private static final String DB_NAME = "MyDB.db";

    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb;
    // UI элементы (кнопки)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigationButtons();

        // Запускаем процесс загрузки и открытия БД асинхронно
        // Передаем в задачу текущий контекст Activity
        new LoadDatabaseTask(this).execute();
    }

    private void setupNavigationButtons() {
        Button btnInstraction = findViewById(R.id.btnInstaction);
        btnInstraction.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, instraction1.class);
            startActivity(intent);
        });

        Button btnRegistration = findViewById(R.id.btnRegistration);
        btnRegistration.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, entryTelegram.class);
            startActivity(intent);
        });

        // Предполагается, что у вас есть кнопка для перехода в Books Activity
        Button btnBooks = findViewById(R.id.btnRegistration);
        if (btnBooks != null) {
            btnBooks.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Books.class);
                startActivity(intent);
            });
        }
    }

    // Внутренний класс AsyncTask для загрузки БД и её открытия
    private class LoadDatabaseTask extends AsyncTask<Void, Void, Boolean> {
        private Context context;

        public LoadDatabaseTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(context, "Проверка и загрузка базы данных...", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            mDBHelper = new DatabaseHelper(context);

            if (mDBHelper.isDatabaseDownloaded()) {
                Log.d(TAG, "База данных уже загружена локально.");
            } else {
                Log.d(TAG, "База данных не найдена локально, скачиваем с Google Drive...");
                // --- РЕАЛЬНЫЙ КОД СКАЧИВАНИЯ ---
                HttpURLConnection connection = null;
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    // *** ИСПРАВЛЕННАЯ СТРОКА URL ***
                    String urlString = "https://drive.google.com/uc?export=download&id=" + FILE_ID;
                    URL url = new URL(urlString); // MalformedURLException больше не возникнет

                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "Server returned HTTP " + connection.getResponseCode());
                        return false;
                    }

                    String dbPathDir = context.getApplicationInfo().dataDir + "/databases/";
                    File dbDir = new File(dbPathDir);
                    if (!dbDir.exists()) dbDir.mkdirs();

                    inputStream = connection.getInputStream();
                    String outFileName = dbPathDir + DB_NAME;
                    outputStream = new FileOutputStream(outFileName);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.flush();
                    Log.d(TAG, "Database downloaded successfully.");
                } catch (IOException e) {
                    Log.e(TAG, "Download failed with exception", e);
                    return false;
                } finally {
                    if (connection != null) connection.disconnect();
                    try {
                        if (outputStream != null) outputStream.close();
                        if (inputStream != null) inputStream.close();
                    } catch (IOException e) { e.printStackTrace(); }
                }
                // --- КОНЕЦ КОДА СКАЧИВАНИЯ ---
            }

            // После того как файл гарантированно существует локально:
            try {
                mDBHelper.openDatabase();
                mDb = mDBHelper.getDatabase();
                if (mDb != null) {
                    Cursor cursor = mDb.rawQuery("SELECT * FROM user LIMIT 1", null);

                    if (cursor.moveToFirst()) {
                        Log.d(TAG, "Данные из БД: Найдена первая запись в таблице user.");
                        cursor.close();
                    } else {
                        Log.d(TAG, "Данные из БД: Таблица user пуста.");
                    }
                }
                return true; // Все прошло успешно

            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при открытии БД после загрузки.", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(context, "Критическая ошибка при подготовке базы данных.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "База данных готова к использованию.", Toast.LENGTH_SHORT).show();
                // БД готова. Пользователь может нажимать на кнопки навигации.
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDBHelper != null) {
            mDBHelper.close();
            Log.d(TAG, "База данных закрыта.");
        }
    }
}
