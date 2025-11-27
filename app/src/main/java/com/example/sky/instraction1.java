package com.example.sky;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class instraction1 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instraction1);

        // 1. Находим первую кнопку по ID из XML-разметки
        Button btnNext = findViewById(R.id.bntNext);

        // Устанавливаем обработчик нажатия для первой кнопки
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем Intent для перехода на Activity instraction
                Intent intent = new Intent(instraction1.this, instraction2.class);
                // Запускаем новое Activity
                startActivity(intent);
            }
        });

    }
}
