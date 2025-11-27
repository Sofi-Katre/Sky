package com.example.sky;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge; // Если вы используете EdgeToEdge
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Music extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music);

        // Находим кнопки по их ID из XML
        ImageView btnSleep = findViewById(R.id.btnSleep);
        //ImageView btnMusic = findViewById(R.id.btnMusic);
        ImageView btnWhether = findViewById(R.id.btnWhether);
        ImageView btnProfile = findViewById(R.id.btnProfile);

        // Устанавливаем обработчики нажатий (Listeners) с использованием лямбда-выражений

        // Переход на страницу Books (Books.java)
        btnSleep.setOnClickListener(v -> {
            Intent intent = new Intent(Music.this, Books.class);
            startActivity(intent);
            // При необходимости можно вызвать finish(), чтобы пользователь не мог вернуться назад
        });

        // Кнопка btnMusic (текущая страница) не имеет обработчика.

        // Переход на страницу Погоды/Главную (entryPage.java)
        btnWhether.setOnClickListener(v -> {
            Intent intent = new Intent(Music.this, entryPage.class);
            startActivity(intent);
        });

        // Переход на страницу Profile (Profile.java)
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Music.this, Profile.class);
            startActivity(intent);
        });

    }
}
