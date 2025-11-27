package com.example.sky;

import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.database.Cursor;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Находим первую кнопку по ID из XML-разметки
        Button btnInstraction = findViewById(R.id.btnInstaction);

        // Устанавливаем обработчик нажатия для первой кнопки
        btnInstraction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем Intent для перехода на Activity instraction
                Intent intent = new Intent(MainActivity.this, instraction1.class);
                // Запускаем новое Activity
                startActivity(intent);
            }
        });

        // 2. Находим вторую кнопку по ID из XML-разметки
        Button btnRegistration = findViewById(R.id.btnRegistration);

        // Устанавливаем обработчик нажатия для второй кнопки
        btnRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем Intent для перехода на Activity entryTelegram
                Intent intent = new Intent(MainActivity.this, entryTelegram.class);
                // Запускаем новое Activity
                startActivity(intent);
            }
        });

        // 3. Использование DatabaseHelper для инициализации базы данных при запуске приложения

        mDBHelper = new DatabaseHelper(this);

        try {
            mDBHelper.createDatabase();
        } catch (IOException e) {
            // В случае ошибки создания/копирования БД, выводим сообщение об ошибке
            Log.e("Database", "Ошибка при создании/копировании БД", e);
            Toast.makeText(this, "Ошибка при подготовке базы данных", Toast.LENGTH_LONG).show();
            // Возможно, здесь стоит остановить дальнейшую работу с БД
        }

        try {
            mDBHelper.openDatabase();
            mDb = mDBHelper.getDatabase();
            Log.d("Database", "База данных открыта успешно.");

            // Теперь вы можете выполнять запросы к mDb
            Cursor cursor = mDb.rawQuery("SELECT * FROM Users", null);
            if (cursor.moveToFirst()) {
                Log.d("Database", "Данные из БД: " + cursor.getString(0));
                cursor.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.makeText(this, "Не удалось открыть базу данных", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDBHelper != null) {
            mDBHelper.close();
            Log.d("Database", "База данных закрыта.");
        }
    }
}
