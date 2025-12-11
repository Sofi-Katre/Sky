package com.example.sky;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Profile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        // Находим кнопки по их ID
        ImageView btnSleep = findViewById(R.id.btnSleep);
        //ImageView btnMusic = findViewById(R.id.btnMusic);
        ImageView btnWhether = findViewById(R.id.btnWhether);
        ImageView btnProfile = findViewById(R.id.btnProfile); // Активная кнопка на текущей странице

        // Устанавливаем обработчики нажатий (Listeners) с использованием лямбда-выражений

        // Переход на страницу Books (entryPage/Books.java, в зависимости от вашей структуры)
        btnSleep.setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, Books.class); // или Books.class
            startActivity(intent);
        });

        // Переход на страницу Music (Music.java)
//        btnMusic.setOnClickListener(v -> {
//            Intent intent = new Intent(Profile.this, Music.class);
//            startActivity(intent);
//        });

        // Переход на страницу Whether/EntryPage (entryPage.java)
        btnWhether.setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, entryPage.class);
            startActivity(intent);
        });

        // Кнопка Profile: мы уже на этой странице, ничего не делаем при клике
        btnProfile.setOnClickListener(v -> {
            // Текущая страница, можно добавить небольшой feedback или оставить пустым
        });
    }
}
