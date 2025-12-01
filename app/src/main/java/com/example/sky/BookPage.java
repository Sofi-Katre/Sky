package com.example.sky;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AudioAttributes;

public class BookPage extends AppCompatActivity implements ChapterAdapter.OnChapterClickListener, Player.Listener {

    private RecyclerView recyclerViewChapters;
    private ChapterAdapter chapterAdapter;
    private List<Chapter> chapterList;
    private DatabaseHelper dbHelper;
    private Book currentBook;
    private ExoPlayer exoPlayer;

    // UI элементы
    private TextView titleBookTextView;
    private TextView authorTextView;
    private TextView genreTextView;
    private TextView dateTextView;
    private ImageView coverImageView;
    private Button btnBack;

    // Текущий воспроизводимый URL (direct)
    private String currentPlayingUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_page);

        dbHelper = new DatabaseHelper(this);

        currentBook = (Book) getIntent().getSerializableExtra("SELECTED_BOOK");

        if (currentBook != null) {
            setupBookDetailsUI(currentBook);
            setupChapterListWithDbData(currentBook.bookId);
        } else {
            Toast.makeText(this, "Ошибка: Данные о книге не найдены", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Инициализируем плеер в onStart
    @Override
    protected void onStart() {
        super.onStart();
        initExoPlayerIfNeeded();
    }

    // Освобождаем ресурсы в onStop
    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
        // Сброс UI списка
        currentPlayingUrl = null;
        if (chapterAdapter != null) chapterAdapter.clearPlaying();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Инициализация ExoPlayer в одном месте
    private void initExoPlayerIfNeeded() {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(this).build();
            exoPlayer.addListener(this);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build();
            exoPlayer.setAudioAttributes(audioAttributes, true);
        }
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            try {
                exoPlayer.release();
            } catch (Exception e) {
                Log.w("BookPage", "Ошибка при освобождении плеера: " + e.getMessage(), e);
            }
            exoPlayer = null;
        }
    }

    // Обработчик клика из адаптера — теперь реализован toggle логикой
    @Override
    public void onChapterClick(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) return;

        // audioUrl здесь ожидается как directUrl, т.к. мы формируем его при чтении из БД
        String directUrl = audioUrl;

        initExoPlayerIfNeeded();

        // Если кликнули по той же главе — переключаем play/pause
        if (directUrl.equals(currentPlayingUrl)) {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.pause();
                // показываем, что сейчас пауза — вернуть иконку Play
                if (chapterAdapter != null) chapterAdapter.clearPlaying();
            } else {
                if (exoPlayer != null) {
                    exoPlayer.play();
                    if (chapterAdapter != null) chapterAdapter.setPlayingUrl(directUrl);
                }
            }
            return;
        }

        // Если кликаем по другой главе — запускаем новую
        playNewAudio(directUrl);
    }

    // Запуск новой главы (стоп предыдущей + запуск новой)
    private void playNewAudio(String directUrl) {
        if (directUrl == null || directUrl.isEmpty()) {
            Toast.makeText(this, "Ссылка на аудиофайл отсутствует.", Toast.LENGTH_SHORT).show();
            return;
        }

        initExoPlayerIfNeeded();

        try {
            // Остановим предыдущую с сохранением позиции (если нужно) — используем stop чтобы начать заново
            if (exoPlayer != null) {
                exoPlayer.stop();
            }

            MediaItem mediaItem = MediaItem.fromUri(directUrl);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();

            // Обновляем UI списка
            currentPlayingUrl = directUrl;
            if (chapterAdapter != null) chapterAdapter.setPlayingUrl(directUrl);

            Toast.makeText(this, "Запускаю воспроизведение...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("BookPage", "Ошибка при подготовке/воспроизведении: " + e.getMessage(), e);
            Toast.makeText(this, "Не удалось воспроизвести файл.", Toast.LENGTH_SHORT).show();
            currentPlayingUrl = null;
            if (chapterAdapter != null) chapterAdapter.clearPlaying();
        }
    }

    private void setupBookDetailsUI(Book book) {
        titleBookTextView = findViewById(R.id.titleBook);
        authorTextView = findViewById(R.id.txtAutore);
        genreTextView = findViewById(R.id.txtJaner);
        dateTextView = findViewById(R.id.txtDate);
        coverImageView = findViewById(R.id.imageView11);
        btnBack = findViewById(R.id.btnBack);

        titleBookTextView.setText(book.title);
        authorTextView.setText("Автор: " + book.author);
        genreTextView.setText("Жанр: " + book.genre);
        dateTextView.setText("Добавлено: " + book.date);

        String directImageUrl = convertGoogleDriveUrlToDirect(book.imageUrl);
        Glide.with(this)
                .load(directImageUrl)
                .placeholder(R.drawable.zaglushka)
                .error(R.drawable.zaglushka)
                .into(coverImageView);

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        Toast.makeText(BookPage.this, "Ошибка воспроизведения.", Toast.LENGTH_SHORT).show();
        Log.e("BookPage", "ExoPlayer error: " + (error != null ? error.getMessage() : "null"));
        currentPlayingUrl = null;
        if (chapterAdapter != null) chapterAdapter.clearPlaying();
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_BUFFERING) {
            // можно показать индикатор загрузки
        } else if (playbackState == Player.STATE_READY) {
            // готово к воспроизведению
        } else if (playbackState == Player.STATE_ENDED) {
            // воспроизведение завершено — сбрасываем индикатор
            currentPlayingUrl = null;
            if (chapterAdapter != null) chapterAdapter.clearPlaying();
        }
    }

    private void setupChapterListWithDbData(int bookId) {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);
        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));

        chapterList = getChaptersForBookFromDB(bookId);

        chapterAdapter = new ChapterAdapter(this, chapterList, this);
        recyclerViewChapters.setAdapter(chapterAdapter);
    }

    /**
     * Формируем список глав для книги.
     * Важно: в вашей БД поле filePath содержит порядковый номер/индекс главы (целое),
     * а поле sequenceOrder содержит короткий Google Drive fileId (строка).
     * Мы читаем filePath как номер главы и sequenceOrder как fileId, формируем direct URL
     * и создаём Chapter(title, duration, directUrl).
     */
    private List<Chapter> getChaptersForBookFromDB(int bookId) {
        List<Chapter> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getDatabase();

        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "База данных недоступна", Toast.LENGTH_SHORT).show();
            return list;
        }

        String selection = "bookId = ?";
        String[] selectionArgs = { String.valueOf(bookId) };

        Cursor cursor = null;
        try {
            // Сортируем по filePath (порядок глав)
            cursor = db.query("audioFile", null, selection, selectionArgs, null, null, "filePath ASC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // filePath — порядковый номер главы (int)
                    int filePath = 1;
                    try {
                        filePath = cursor.getInt(cursor.getColumnIndexOrThrow("filePath"));
                    } catch (Exception e) {
                        // на случай, если в filePath хранится строка — попытка парсинга
                        try {
                            String fp = cursor.getString(cursor.getColumnIndexOrThrow("filePath"));
                            filePath = Integer.parseInt(fp);
                        } catch (Exception ignored) {
                        }
                    }

                    String title = "Глава " + filePath;
                    String duration = "00:00";

                    // sequenceOrder здесь — короткий id от Google Drive (fileId)
                    String fileId = null;
                    try {
                        fileId = cursor.getString(cursor.getColumnIndexOrThrow("sequenceOrder"));
                    } catch (Exception e) {
                        Log.w("BookPage", "sequenceOrder not found or null for bookId=" + bookId, e);
                    }

                    String directAudioUrl = "";
                    if (fileId != null && !fileId.trim().isEmpty()) {
                        directAudioUrl = convertGoogleDriveUrlToDirect(fileId);
                    }

                    Log.i("BookPage", "Added chapter: title=" + title + ", fileId=" + fileId + ", directUrl=" + directAudioUrl);

                    list.add(new Chapter(title, duration, directAudioUrl));
                }
            }
        } catch (Exception e) {
            Log.e("BookPage", "Ошибка чтения из БД: " + e.getMessage(), e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return list;
    }

    // Конвертируем разные варианты ссылок/ID Google Drive в прямую ссылку
    private String convertGoogleDriveUrlToDirect(String fullUrlOrId) {
        if (fullUrlOrId == null || fullUrlOrId.isEmpty()) {
            return "";
        }

        try {
            String fileId = fullUrlOrId;

            if (fullUrlOrId.contains("/d/")) {
                int startIndex = fullUrlOrId.indexOf("/d/") + 3;
                int endIndex = fullUrlOrId.indexOf('/', startIndex);
                if (endIndex == -1) endIndex = fullUrlOrId.length();
                fileId = fullUrlOrId.substring(startIndex, endIndex);
            } else if (fullUrlOrId.contains("open?id=")) {
                int idx = fullUrlOrId.indexOf("open?id=") + 8;
                fileId = fullUrlOrId.substring(idx);
            } else if (fullUrlOrId.contains("id=") && fullUrlOrId.contains("drive.google.com")) {
                int idx = fullUrlOrId.indexOf("id=") + 3;
                fileId = fullUrlOrId.substring(idx);
            } else if (fullUrlOrId.startsWith("http") && fullUrlOrId.contains("drive.google.com")) {
                fileId = fullUrlOrId.substring(fullUrlOrId.lastIndexOf('/') + 1);
            } else {
                fileId = fullUrlOrId;
            }

            return "https://drive.google.com/uc?export=download&id=" + fileId;
        } catch (Exception e) {
            Log.e("BookPage", "Failed to parse Google Drive URL/ID: " + fullUrlOrId, e);
            return fullUrlOrId;
        }
    }
}
