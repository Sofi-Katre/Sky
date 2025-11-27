package com.example.sky;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class entryTelegram extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);

        // 1. Находим первую кнопку по ID из XML-разметки
        Button btnEntry = findViewById(R.id.btnEntry);

        // Устанавливаем обработчик нажатия для первой кнопки
        btnEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем Intent для перехода на Activity instraction
                Intent intent = new Intent(entryTelegram.this, entryPage.class);
                // Запускаем новое Activity
                startActivity(intent);
            }
        });
    }
}
