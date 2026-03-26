package com.example.sky;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Books extends AppCompatActivity implements BooksAdapter.OnBookClickListener {

    private static final String TAG = "BooksActivity";

    private RecyclerView recyclerView;
    private BooksAdapter adapter;
    private DatabaseHelper dbHelper;

    private ImageView btnJaner, btnClassic, btnKids, btnDetective;
    private ImageView btnFilterNew, btnFilterAlphabet;

    private String currentSortMode = "date"; // "date" или "alpha"
    private String currentGenreFilter = null; // null = все жанры

    private Set<String> favoriteIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books);

        dbHelper = new DatabaseHelper(this);

        setupFilterButtons();
        setupSortButtons();
        initializeUIAndLoadBooks();
        setupNavigationButtons();
        loadFavoritesFromFirebase();
    }

    /** ------------------- Фильтры по жанрам ------------------- */
    private void setupFilterButtons() {
        btnJaner = findViewById(R.id.btnJaner);
        btnClassic = findViewById(R.id.btnClassic);
        btnKids = findViewById(R.id.btnKids);
        btnDetective = findViewById(R.id.btnDetective);

        btnJaner.setOnClickListener(v -> {
            updateGenreUI("All");
            currentGenreFilter = null;
            loadBooksByGenre();
        });
        btnClassic.setOnClickListener(v -> {
            updateGenreUI("Classic");
            currentGenreFilter = "Классика";
            loadBooksByGenre();
        });
        btnKids.setOnClickListener(v -> {
            updateGenreUI("Kids");
            currentGenreFilter = "Детям";
            loadBooksByGenre();
        });
        btnDetective.setOnClickListener(v -> {
            updateGenreUI("Detective");
            currentGenreFilter = "Детектив";
            loadBooksByGenre();
        });

        updateGenreUI("All");
    }

    private void updateGenreUI(String selectedGenre) {
        btnJaner.setImageResource(R.drawable.btnalloff);
        btnClassic.setImageResource(R.drawable.btnclassicblack);
        btnKids.setImageResource(R.drawable.btnkidsblack);
        btnDetective.setImageResource(R.drawable.btndetectiveblack);

        switch (selectedGenre) {
            case "All": btnJaner.setImageResource(R.drawable.btnall); break;
            case "Classic": btnClassic.setImageResource(R.drawable.btnclassic); break;
            case "Kids": btnKids.setImageResource(R.drawable.btnkids); break;
            case "Detective": btnDetective.setImageResource(R.drawable.btndetective); break;
        }
    }

    /** ------------------- Сортировка ------------------- */
    private void setupSortButtons() {
        btnFilterNew = findViewById(R.id.btnFilterNew);
        btnFilterAlphabet = findViewById(R.id.btnFilterAlphabet);

        btnFilterNew.setOnClickListener(v -> {
            currentSortMode = "date";
            updateSortUI();
            loadBooksByGenre();
        });

        btnFilterAlphabet.setOnClickListener(v -> {
            currentSortMode = "alpha";
            updateSortUI();
            loadBooksByGenre();
        });

        updateSortUI();
    }

    private void updateSortUI() {
        if ("date".equals(currentSortMode)) {
            btnFilterNew.setImageResource(R.drawable.filtertonew);
            btnFilterAlphabet.setImageResource(R.drawable.filtertoalfavitoff);
        } else {
            btnFilterNew.setImageResource(R.drawable.filtertonewoff);
            btnFilterAlphabet.setImageResource(R.drawable.filtertoalfavit);
        }
    }

    /** ------------------- RecyclerView и загрузка книг ------------------- */
    private void initializeUIAndLoadBooks() {
        try {
            if (dbHelper.getDatabase() == null || !dbHelper.getDatabase().isOpen()) {
                dbHelper.openDatabase();
            }

            recyclerView = findViewById(R.id.recyclerViewBooks);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            adapter = new BooksAdapter(this, loadBooksFromDB(), this);
            recyclerView.setAdapter(adapter);

        } catch (SQLException e) {
            Log.e(TAG, "Ошибка при открытии БД", e);
            Toast.makeText(this, "Не удалось открыть базу данных.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadBooksByGenre() {
        if (adapter != null) {
            adapter.updateList(loadBooksFromDB());
        }
    }

    private void loadFavoritesFromFirebase() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore.getInstance().collection("favorites").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Обновляем наш список ID избранных книг
                        favoriteIds = documentSnapshot.getData().keySet();

                        // Перезагружаем список из локальной БД, чтобы обновить статусы isFavorite
                        List<Book> updatedList = loadBooksFromDB();
                        if (adapter != null) {
                            adapter.updateList(updatedList);
                        }
                    }
                });
    }

    /** ------------------- Загрузка книг из БД ------------------- */
    private List<Book> loadBooksFromDB() {
        List<Book> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getDatabase();
        if (db == null || !db.isOpen()) return list;

        Cursor cursor = null;
        try {
            String selection = null;
            String[] selectionArgs = null;

            if (currentGenreFilter != null) {
                selection = "book_genre = ?";
                selectionArgs = new String[]{currentGenreFilter};
            }

            String orderBy = "date".equals(currentSortMode) ? "bookId DESC" : "book_name COLLATE LOCALIZED ASC";

            cursor = db.query("Book", null, selection, selectionArgs, null, null, orderBy);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("bookId"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("book_name"));
                String author = cursor.getString(cursor.getColumnIndexOrThrow("autore"));
                String genre = cursor.getString(cursor.getColumnIndexOrThrow("book_genre"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("book_date"));
                String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("book_image_url"));

                Book book = new Book(id, title, author, genre, date, imageUrl);
                book.setFavorite(favoriteIds.contains(String.valueOf(id)));
                list.add(book);
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при чтении данных из БД", e);
        } finally {
            if (cursor != null) cursor.close();
        }

        return list;
    }

    /** ------------------- Клик по книге ------------------- */
    @Override
    public void onBookClick(Book book) {
        Intent intent = new Intent(this, BookPage.class);
        intent.putExtra("SELECTED_BOOK", book);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Book book) {
        toggleFavorite(book); // Вызываем твой метод Firebase, который мы уже обсудили
    }

    /** ------------------- Кнопка избранного (Firebase) ------------------- */
    public void toggleFavorite(Book book) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference favRef = db.collection("favorites").document(userId);

        // 1. СРАЗУ меняем состояние в объекте и обновляем экран (мгновенный отклик)
        boolean newState = !book.isFavorite();
        book.setFavorite(newState);
        adapter.notifyDataSetChanged();

        // 2. В фоне отправляем запрос в Firebase
        Map<String, Object> update = new HashMap<>();
        if (newState) {
            update.put(String.valueOf(book.bookId), true);
        } else {
            update.put(String.valueOf(book.bookId), FieldValue.delete());
        }

        favRef.set(update, SetOptions.merge())
                .addOnFailureListener(e -> {
                    // Если вдруг ошибка интернета — возвращаем иконку назад
                    book.setFavorite(!newState);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Ошибка сохранения!", Toast.LENGTH_SHORT).show();
                });
    }

    /** ------------------- Нижняя навигация ------------------- */
    private void setupNavigationButtons() {
        ImageView btnWhether = findViewById(R.id.btnWhether);
        ImageView btnProfile = findViewById(R.id.btnProfile);

        btnWhether.setOnClickListener(v -> startActivity(new Intent(Books.this, entryPage.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(Books.this, Profile.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}