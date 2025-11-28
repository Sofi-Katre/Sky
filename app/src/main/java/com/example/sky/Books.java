package com.example.sky;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class Books extends AppCompatActivity {

    private static final String TAG = "BooksActivity";
    private RecyclerView recyclerView;
    private BooksAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books);

        dbHelper = new DatabaseHelper(this);

        initializeUIAndLoadBooks();

        // Обработчики навигации остаются на месте
        setupNavigationButtons();
    }

    private void setupNavigationButtons() {
        ImageView btnMusic = findViewById(R.id.btnMusic);
        ImageView btnWhether = findViewById(R.id.btnWhether);
        ImageView btnProfile = findViewById(R.id.btnProfile);

        btnMusic.setOnClickListener(v -> {
            Intent intent = new Intent(Books.this, Music.class);
            startActivity(intent);
        });
        btnWhether.setOnClickListener(v -> {
            Intent intent = new Intent(Books.this, entryPage.class);
            startActivity(intent);
        });
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Books.this, Profile.class);
            startActivity(intent);
        });
    }

    private void initializeUIAndLoadBooks() {
        try {
            // *** 1. Убеждаемся, что БД открыта ***
            if (dbHelper.getDatabase() == null || !dbHelper.getDatabase().isOpen()) {
                dbHelper.openDatabase();
            }

            // *** 2. Инициализируем RecyclerView ***
            recyclerView = findViewById(R.id.recyclerViewBooks);
            if (recyclerView == null) {
                Log.e(TAG, "recyclerViewBooks not found in the layout!");
                return;
            }
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // *** 3. Загружаем данные и подключаем адаптер ***
            List<Book> books = getAllBooksFromDB();
            adapter = new BooksAdapter(this, books);
            recyclerView.setAdapter(adapter);

        } catch (SQLException e) {
            // Эта ошибка сработает, если MainActivity не смогла скачать файл по какой-то причине
            Log.e(TAG, "Error opening database or loading books", e);
            Toast.makeText(this, "Не удалось открыть базу данных. Убедитесь, что она была загружена.", Toast.LENGTH_LONG).show();
        }
    }

    // --- Удален класс DownloadDatabaseTask ---

    // Рекомендуется закрывать базу данных при уничтожении активности
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private List<Book> getAllBooksFromDB() {
        List<Book> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getDatabase();

        if (db == null || !db.isOpen()) {
            Log.e(TAG, "Database is not open when trying to fetch data.");
            return list;
        }

        Cursor cursor = db.query("Book", null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("bookId"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("book_name"));
            String author = cursor.getString(cursor.getColumnIndexOrThrow("autore"));
            String genre = cursor.getString(cursor.getColumnIndexOrThrow("book_genre"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("book_date"));
            String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("book_image_url"));

            Book book = new Book(id, title, author, genre, date, imageUrl);
            list.add(book);
        }
        cursor.close();
        return list;
    }
}
