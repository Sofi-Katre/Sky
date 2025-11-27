package com.example.sky;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.sky.Book;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.bumptech.glide.Glide; // Убедитесь, что эта библиотека добавлена в Gradle
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Books extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BooksAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.books);

        dbHelper = new DatabaseHelper(this);
        try {
            dbHelper.createDatabase();
            dbHelper.openDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }

        recyclerView = findViewById(R.id.recyclerViewBooks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Book> books = getAllBooksFromDB();

        // ИСПРАВЛЕНИЕ: Используем конструктор внешнего адаптера,
        // который принимает Context (this)
        adapter = new BooksAdapter(this, books);
        recyclerView.setAdapter(adapter);

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
