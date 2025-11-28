package com.example.sky;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sky.ChapterAdapter;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class BookPage extends AppCompatActivity {

    private RecyclerView recyclerViewChapters;
    private ChapterAdapter chapterAdapter;
    private List<Chapter> chapterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_page); // Убедитесь, что здесь правильный файл макета

        // ... (инициализация других элементов, например, кнопок плеера) ...

        setupChapterList();
    }

    private void setupChapterList() {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);
        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));

        // Заполните список данными (пример: получение из БД или вручную)
        chapterList = getChaptersForBook();

        chapterAdapter = new ChapterAdapter(this, chapterList);
        recyclerViewChapters.setAdapter(chapterAdapter);
    }

    // Пример получения списка глав
    private List<Chapter> getChaptersForBook() {
        List<Chapter> list = new ArrayList<>();
        // Здесь вы должны получить реальные данные из вашей базы данных SQLite
        list.add(new Chapter("Глава 1. Начало", "32 мин"));
        list.add(new Chapter("Глава 2. Приключения", "28 мин"));
        list.add(new Chapter("Глава 3. Развязка", "53 мин"));
        // ... добавьте остальные главы из БД ...
        return list;
    }
}
