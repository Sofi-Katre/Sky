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

    private static final String FILE_ID = "1d5r568tZCQazY7OJKNfnay5F77eAytHk";
    private static final String TAG = "MainActivity";
    private static final String DB_NAME = "MyDB.db";

    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupNavigationButtons();

        // Запускаем загрузку базы
        new LoadDatabaseTask(this).execute();
    }

    private void setupNavigationButtons() {
        Button btnInstraction = findViewById(R.id.btnInstaction);
        if (btnInstraction != null) {
            btnInstraction.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, instraction1.class)));
        }

        Button btnRegistration = findViewById(R.id.btnRegistration);
        if (btnRegistration != null) {
            btnRegistration.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, entryGoogle.class));
                finish();
            });
        }
    }

    private class LoadDatabaseTask extends AsyncTask<Void, Void, Boolean> {
        private final Context context;

        LoadDatabaseTask(Context context) { this.context = context; }

        @Override
        protected void onPreExecute() {
            Toast.makeText(context, "Проверка и загрузка базы данных...", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            mDBHelper = new DatabaseHelper(context);

            boolean dbReady = false;

            // Попытка скачать с Google Drive
            if (!mDBHelper.isDatabaseDownloaded()) {
                Log.d(TAG, "База данных не найдена локально, пробуем скачать с Google Drive...");
                dbReady = downloadDatabaseFromDrive(context);

                if (!dbReady) {
                    Log.w(TAG, "Не удалось скачать с Google Drive, используем локальную базу из assets.");
                    dbReady = copyDatabaseFromAssets(context);
                }
            } else {
                Log.d(TAG, "База данных уже локально.");
                dbReady = true;
            }

            if (!dbReady) return false;

            // Открываем базу
            try {
                mDBHelper.openDatabase();
                mDb = mDBHelper.getDatabase();
                if (mDb != null) {
                    Cursor cursor = null;
                    try {
                        cursor = mDb.rawQuery("SELECT * FROM user LIMIT 1", null);
                        if (cursor != null && cursor.moveToFirst()) {
                            Log.d(TAG, "Данные из БД: найдена первая запись.");
                        } else {
                            Log.d(TAG, "Таблица user пуста.");
                        }
                    } finally {
                        if (cursor != null) cursor.close();
                    }
                }
                return true;
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при открытии базы данных", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(context, "Критическая ошибка при подготовке базы данных.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "База данных готова к использованию.", Toast.LENGTH_SHORT).show();
            }
        }

        // -----------------------------
        // Скачивание с Google Drive
        private boolean downloadDatabaseFromDrive(Context context) {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                String urlString = "https://drive.google.com/uc?export=download&id=" + FILE_ID;
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                int responseCode = connection.getResponseCode();
                String contentType = connection.getContentType();

                if (!"application/x-sqlite3".equals(contentType) &&
                        !"application/octet-stream".equals(contentType)) {
                    Log.e(TAG, "Google Drive вернул HTML или неподходящий файл: " + contentType);
                    return false;
                }

                File dbDir = new File(context.getApplicationInfo().dataDir + "/databases/");
                if (!dbDir.exists()) dbDir.mkdirs();

                String outFileName = dbDir + "/" + DB_NAME;
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(outFileName);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();

                Log.d(TAG, "База успешно скачана с Google Drive!");
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при скачивании базы с Google Drive", e);
                return false;
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // -----------------------------
        // Копирование базы из assets
        private boolean copyDatabaseFromAssets(Context context) {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = context.getAssets().open(DB_NAME);

                File dbDir = new File(context.getApplicationInfo().dataDir + "/databases/");
                if (!dbDir.exists()) dbDir.mkdirs();

                String outFileName = dbDir + "/" + DB_NAME;
                outputStream = new FileOutputStream(outFileName);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();

                Log.d(TAG, "База скопирована из assets.");
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при копировании базы из assets", e);
                return false;
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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