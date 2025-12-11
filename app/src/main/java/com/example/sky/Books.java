package com.example.sky;

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

import java.util.ArrayList;
import java.util.List;

public class Books extends AppCompatActivity implements BooksAdapter.OnBookClickListener {

    private static final String TAG = "BooksActivity";
    private RecyclerView recyclerView;
    private BooksAdapter adapter;
    private DatabaseHelper dbHelper;

    private ImageView btnJaner, btnClassic, btnKids, btnDetective;
    // НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ КНОПОК СОРТИРОВКИ
    private ImageView btnFilterNew, btnFilterAlphabet;

    // Переменная для хранения текущего режима сортировки ("date" или "alpha")
    private String currentSortMode = "date"; // Изначально сортируем по дате (новизне)
    // Переменная для хранения текущего активного жанра (изначально null - все жанры)
    private String currentGenreFilter = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books);

        dbHelper = new DatabaseHelper(this);

        // 1. Инициализируем кнопки фильтров и устанавливаем слушатели
        setupFilterButtons();

        // 2. Инициализируем кнопки сортировки и устанавливаем слушатели
        setupSortButtons();

        // 3. Инициализируем UI (RecyclerView) и загружаем книги
        initializeUIAndLoadBooks();

        // 4. Инициализируем нижние навигационные кнопки
        setupNavigationButtons();
    }

    private void setupNavigationButtons() {
        //ImageView btnMusic = findViewById(R.id.btnMusic);
        ImageView btnWhether = findViewById(R.id.btnWhether);
        ImageView btnProfile = findViewById(R.id.btnProfile);

        //... (слушатели навигации)
        btnWhether.setOnClickListener(v -> {
            Intent intent = new Intent(Books.this, entryPage.class);
            startActivity(intent);
        });
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Books.this, Profile.class);
            startActivity(intent);
        });
    }

    // НОВЫЙ МЕТОД: Инициализация кнопок сортировки и установка слушателей
    private void setupSortButtons() {
        btnFilterNew = findViewById(R.id.btnFilterNew);
        btnFilterAlphabet = findViewById(R.id.btnFilterAlphabet);

        btnFilterNew.setOnClickListener(v -> {
            currentSortMode = "date"; // Сортировка по дате (новизне)
            updateSortUI(currentSortMode);
            loadBooksByGenre(); // Перезагружаем список с новым режимом сортировки
        });

        btnFilterAlphabet.setOnClickListener(v -> {
            currentSortMode = "alpha"; // Сортировка по алфавиту
            updateSortUI(currentSortMode);
            loadBooksByGenre(); // Перезагружаем список с новым режимом сортировки
        });

        // Устанавливаем начальное состояние UI сортировки (по умолчанию активна кнопка "Новые")
        updateSortUI(currentSortMode);
    }

    // НОВЫЙ МЕТОД: Обновление внешнего вида кнопок сортировки
    private void updateSortUI(String sortMode) {
        if ("date".equals(sortMode)) {
            // Активна кнопка "Новые", неактивна "Алфавит"
            btnFilterNew.setImageResource(R.drawable.filtertonew); // Активное изображение для "Новые"
            btnFilterAlphabet.setImageResource(R.drawable.filtertoalfavitoff); // Неактивное изображение для "Алфавит"
        } else if ("alpha".equals(sortMode)) {
            // Активна кнопка "Алфавит", неактивна "Новые"
            btnFilterNew.setImageResource(R.drawable.filtertonewoff); // Неактивное изображение для "Новые" (Вам нужно добавить этот ресурс!)
            btnFilterAlphabet.setImageResource(R.drawable.filtertoalfavit); // Активное изображение для "Алфавит"
        }
    }


    /**
     * Инициализация кнопок фильтрации и установка слушателей кликов
     */
    private void setupFilterButtons() {
        btnJaner = findViewById(R.id.btnJaner);
        btnClassic = findViewById(R.id.btnClassic);
        btnKids = findViewById(R.id.btnKids);
        btnDetective = findViewById(R.id.btnDetective);

        btnJaner.setOnClickListener(v -> {
            updateGenreUI("All");
            currentGenreFilter = null; // null означает загрузить все жанры
            loadBooksByGenre(); // Вызываем метод без параметров
        });

        btnClassic.setOnClickListener(v -> {
            updateGenreUI("Classic");
            currentGenreFilter = "Классика"; // Передаем точное русское название из БД
            loadBooksByGenre(); // Вызываем метод без параметров
        });

        btnKids.setOnClickListener(v -> {
            updateGenreUI("Kids");
            currentGenreFilter = "Детям"; // Передаем точное русское название из БД
            loadBooksByGenre(); // Вызываем метод без параметров
        });

        btnDetective.setOnClickListener(v -> {
            updateGenreUI("Detective");
            currentGenreFilter = "Детектив"; // Передаем точное русское название из БД
            loadBooksByGenre(); // Вызываем метод без параметров
        });

        // Устанавливаем начальное состояние UI (кнопка "Все" активна)
        updateGenreUI("All");
    }

    /**
     * Обновляет внешний вид кнопок жанров (подсветка активной кнопки)
     */
    private void updateGenreUI(String selectedGenre) {
        // Сброс всех кнопок в неактивное состояние
        btnJaner.setImageResource(R.drawable.btnalloff); // Используем неактивный ресурс btnAlloff
        btnClassic.setImageResource(R.drawable.btnclassicblack);
        btnKids.setImageResource(R.drawable.btnkidsblack);
        btnDetective.setImageResource(R.drawable.btndetectiveblack);

        // Установка активного состояния (цветные иконки) для выбранного жанра
        switch (selectedGenre) {
            case "All":
                btnJaner.setImageResource(R.drawable.btnall);
                break;
            case "Classic":
                btnClassic.setImageResource(R.drawable.btnclassic);
                break;
            case "Kids":
                btnKids.setImageResource(R.drawable.btnkids);
                break;
            case "Detective":
                btnDetective.setImageResource(R.drawable.btndetective);
                break;
        }
    }

    /**
     * Вызывается кнопками фильтрации/сортировки для загрузки нужных книг
     */
    private void loadBooksByGenre() {
        // Используем унифицированный метод loadBooksFromDB, который использует глобальные переменные
        List<Book> filteredList = loadBooksFromDB();
        if (adapter != null) {
            adapter.updateList(filteredList);
        }
    }

    private void initializeUIAndLoadBooks() {
        try {
            if (dbHelper.getDatabase() == null || !dbHelper.getDatabase().isOpen()) {
                dbHelper.openDatabase();
            }

            recyclerView = findViewById(R.id.recyclerViewBooks);
            if (recyclerView == null) {
                Log.e(TAG, "recyclerViewBooks not found in the layout!");
                return;
            }
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Загружаем все книги при старте с сортировкой по умолчанию ("date")
            List<Book> books = loadBooksFromDB();

            adapter = new BooksAdapter(this, books, this);
            recyclerView.setAdapter(adapter);

        } catch (SQLException e) {
            Log.e(TAG, "Error opening database or loading books", e);
            Toast.makeText(this, "Не удалось открыть базу данных. Убедитесь, что она была загружена.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Унифицированный метод загрузки книг из БД с фильтром И СОРТИРОВКОЙ
     */
    private List<Book> loadBooksFromDB() {
        List<Book> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getDatabase();

        if (db == null || !db.isOpen()) {
            Log.e(TAG, "Database is not open when trying to fetch data.");
            return list;
        }

        Cursor cursor = null;
        try {
            String selection = null;
            String[] selectionArgs = null;

            // Определяем фильтр по текущему состоянию currentGenreFilter
            if (currentGenreFilter != null) {
                selection = "book_genre = ?";
                selectionArgs = new String[]{currentGenreFilter};
            }

            // Определяем режим сортировки ORDER BY по текущему состоянию currentSortMode
            String orderBy = null;
            if ("date".equals(currentSortMode)) {
                // Сортировка по дате добавления (предполагаем, что bookId растет с добавлением)
                orderBy = "bookId DESC";
            } else if ("alpha".equals(currentSortMode)) {
                // Сортировка по названию книги в алфавитном порядке
                orderBy = "book_name COLLATE LOCALIZED ASC"; // Используем COLLATE LOCALIZED для русского алфавита
            }

            cursor = db.query("Book", null, selection, selectionArgs, null, null, orderBy);

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
        } catch (Exception e) {
            Log.e(TAG, "Error while reading book data from cursor", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    // Обработка клика по элементу списка (карточке книги)
    @Override
    public void onBookClick(Book book) {
        Intent intent = new Intent(this, BookPage.class);
        intent.putExtra("SELECTED_BOOK", book);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
