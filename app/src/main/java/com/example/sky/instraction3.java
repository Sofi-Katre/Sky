package com.example.sky;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class instraction3 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instraction3);

        // 1. Находим первую кнопку по ID из XML-разметки
        Button btnNext = findViewById(R.id.bntNext);

        // Устанавливаем обработчик нажатия для первой кнопки
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем Intent для перехода на Activity instraction
                Intent intent = new Intent(instraction3.this, instraction4.class);
                // Запускаем новое Activity
                startActivity(intent);
            }
        });
    }
}
