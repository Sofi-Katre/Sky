package com.example.sky;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class entryPage extends AppCompatActivity {

    private RecyclerView recyclerViewFavorites;
    private FavoritesAdapter favoritesAdapter;
    private DatabaseHelper dbHelper;

    // -------------------- фильтры и сортировка --------------------
    private String currentGenreFilter = null; // null = все жанры
    private String currentSortMode = "date"; // "date" или "alpha"

    // -------------------- кнопки --------------------
    private ImageView btnJaner, btnClassic, btnKids, btnDetective;
    private ImageView btnFilterNew, btnFilterAlphabet;

    // -------------------- ресурсы для визуала --------------------
    private int resBtnAllActive = R.drawable.btnall;
    private int resBtnAllInactive = R.drawable.btnalloff;

    private int resBtnClassicActive = R.drawable.btnclassic;
    private int resBtnClassicInactive = R.drawable.btnclassicblack;

    private int resBtnKidsActive = R.drawable.btnkids;
    private int resBtnKidsInactive = R.drawable.btnkidsblack;

    private int resBtnDetectiveActive = R.drawable.btndetective;
    private int resBtnDetectiveInactive = R.drawable.btndetectiveblack;

    private int resSortDateActive = R.drawable.filtertonew;
    private int resSortDateInactive = R.drawable.filtertonewoff;

    private int resSortAlphaActive = R.drawable.filtertoalfavit;
    private int resSortAlphaInactive = R.drawable.filtertoalfavitoff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ----------------- Проверка входа -----------------
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            startActivity(new Intent(this, entryGoogle.class));
            finish();
            return;
        }

        setContentView(R.layout.entry_page);

        dbHelper = new DatabaseHelper(this);

        // ----------------- RecyclerView -----------------
        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));

        // ----------------- Кнопки -----------------
        setupFilterButtons();

        // ----------------- Нижняя навигация -----------------
        setupNavigation();

        // ----------------- Загрузка избранного -----------------
        loadFavoriteBooks();
    }

    // ----------------- Настройка кнопок фильтров и сортировки -----------------
    private void setupFilterButtons() {
        btnJaner = findViewById(R.id.btnJaner);
        btnClassic = findViewById(R.id.imageView5);
        btnKids = findViewById(R.id.imageView6);
        btnDetective = findViewById(R.id.imageView7);

        btnFilterNew = findViewById(R.id.imageView9);
        btnFilterAlphabet = findViewById(R.id.imageView10);

        // ----------------- Жанры -----------------
        btnJaner.setOnClickListener(v -> {
            currentGenreFilter = null;
            applyFilters();
            updateButtonUI();
        });

        btnClassic.setOnClickListener(v -> {
            currentGenreFilter = "Классика";
            applyFilters();
            updateButtonUI();
        });

        btnKids.setOnClickListener(v -> {
            currentGenreFilter = "Детям";
            applyFilters();
            updateButtonUI();
        });

        btnDetective.setOnClickListener(v -> {
            currentGenreFilter = "Детектив";
            applyFilters();
            updateButtonUI();
        });

        // ----------------- Сортировка -----------------
        btnFilterNew.setOnClickListener(v -> {
            currentSortMode = "date";
            applyFilters();
            updateButtonUI();
        });

        btnFilterAlphabet.setOnClickListener(v -> {
            currentSortMode = "alpha";
            applyFilters();
            updateButtonUI();
        });
    }

    // ----------------- Обновление UI кнопок -----------------
    private void updateButtonUI() {
        // Жанры
        btnJaner.setImageResource(currentGenreFilter == null ? resBtnAllActive : resBtnAllInactive);
        btnClassic.setImageResource("Классика".equals(currentGenreFilter) ? resBtnClassicActive : resBtnClassicInactive);
        btnKids.setImageResource("Детям".equals(currentGenreFilter) ? resBtnKidsActive : resBtnKidsInactive);
        btnDetective.setImageResource("Детектив".equals(currentGenreFilter) ? resBtnDetectiveActive : resBtnDetectiveInactive);

        // Сортировка
        btnFilterNew.setImageResource("date".equals(currentSortMode) ? resSortDateActive : resSortDateInactive);
        btnFilterAlphabet.setImageResource("alpha".equals(currentSortMode) ? resSortAlphaActive : resSortAlphaInactive);
    }

    // ----------------- Применение фильтров -----------------
    private void applyFilters() {
        if (favoritesAdapter != null) {
            favoritesAdapter.applyFilters(currentGenreFilter, currentSortMode);
        }
    }

    // ----------------- Загрузка избранного из Firebase -----------------
    private void loadFavoriteBooks() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore.getInstance()
                .collection("favorites")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getData() != null) {

                        Set<String> favSet = document.getData().keySet();

                        List<Integer> favoriteIds = new ArrayList<>();
                        for (String idStr : favSet) {
                            try {
                                favoriteIds.add(Integer.parseInt(idStr));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        List<Book> favoriteBooks = dbHelper.getBooksByIds(favoriteIds);

                        if (favoritesAdapter == null) {
                            favoritesAdapter = new FavoritesAdapter(favoriteBooks, this);
                            recyclerViewFavorites.setAdapter(favoritesAdapter);
                        } else {
                            favoritesAdapter.updateData(favoriteBooks);
                        }

                        // 🔥 Сразу применяем фильтры и обновляем UI кнопок
                        applyFilters();
                        updateButtonUI();
                    }
                })
                .addOnFailureListener(e -> Log.e("entryPage", "Ошибка Firebase", e));
    }

    // ----------------- Нижняя навигация -----------------
    private void setupNavigation() {
        ImageView btnSleep = findViewById(R.id.btnSleep);
        ImageView btnProfile = findViewById(R.id.btnProfile);

        btnSleep.setOnClickListener(v ->
                startActivity(new Intent(entryPage.this, Books.class)));

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(entryPage.this, Profile.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteBooks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}