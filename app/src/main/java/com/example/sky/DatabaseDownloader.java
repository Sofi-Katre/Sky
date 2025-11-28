package com.example.sky;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DatabaseDownloader {

    private static final String TAG = "DatabaseDownloader";
    // ID вашего файла из ссылки Google Drive
    private static final String FILE_ID = "1TKyECifFSflpaEyHY044oiQjQi9_zb9P";

    // *** ИСПРАВЛЕННАЯ СТРОКА С ПРАВИЛЬНЫМ URL-АДРЕСОМ ***
    private static final String DOWNLOAD_URL = "https://drive.google.com" + FILE_ID;
    // ******************************************************

    public static File downloadDatabaseFile(Context context, String dbFileName) throws IOException {
        File dbPath = context.getDatabasePath(dbFileName);

        if (!dbPath.getParentFile().exists()) {
            dbPath.getParentFile().mkdirs();
        }

        // Опционально: если файл уже есть, не скачиваем его повторно
        // if (dbPath.exists()) { return dbPath; }

        URL url = new URL(DOWNLOAD_URL);
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode());
                throw new IOException("Failed to download file: HTTP " + connection.getResponseCode());
            }

            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(dbPath);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            Log.d(TAG, "Database file downloaded to: " + dbPath.getAbsolutePath());
            return dbPath;

        } finally {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (connection != null) connection.disconnect();
        }
    }
}
