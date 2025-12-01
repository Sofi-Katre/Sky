package com.example.sky;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import android.widget.Button;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // UI для плеера
    private TextView txtCurrentTime;
    private TextView txtTotalTime;
    private SeekBar seekBarProgress;
    private ImageView btnPlayPause;
    private ImageView btnRewind10;
    private ImageView btnForward15;

    // Кэш длительностей аудиофайлов
    private Map<String, Long> durationCache = new HashMap<>();

    // Текущий воспроизводимый URL
    private String currentPlayingUrl = null;

    // Переменные для отслеживания состояния плеера
    private boolean isPlaying = false;

    // Состояние кнопок для глав
    private String currentChapterState = "stop"; // "stop", "play", "pause"

    // Обработчик для обновления UI каждую секунду
    private Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateUIRunnable = new Runnable() {
        @Override
        public void run() {
            updatePlayerUI();
            handler.postDelayed(this, 1000); // обновляем каждую секунду
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_page);

        dbHelper = new DatabaseHelper(this);

        // Инициализация UI
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind10 = findViewById(R.id.btnRewind10);
        btnForward15 = findViewById(R.id.btnForward15);

        // Кнопки управления
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnRewind10.setOnClickListener(v -> seekBackward10Sec());
        btnForward15.setOnClickListener(v -> seekForward15Sec());

        currentBook = (Book) getIntent().getSerializableExtra("SELECTED_BOOK");

        if (currentBook != null) {
            setupBookDetailsUI(currentBook);
            setupChapterListWithDbData(currentBook.bookId);
        } else {
            Toast.makeText(this, "Ошибка: Данные о книге не найдены", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupChapterListWithDbData(int bookId) {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters); // Найдем RecyclerView по ID
        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this)); // Настроим менеджер компоновки (LinearLayoutManager)

        // Получим список глав из базы данных
        chapterList = getChaptersForBookFromDB(bookId);

        // Создаем адаптер и назначаем его для RecyclerView
        chapterAdapter = new ChapterAdapter(this, chapterList, this); // передаем слушатель (this) для обработки кликов
        recyclerViewChapters.setAdapter(chapterAdapter); // Устанавливаем адаптер в RecyclerView
    }

    @Override
    protected void onStart() {
        super.onStart();
        initExoPlayerIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
        currentPlayingUrl = null;
        if (chapterAdapter != null) chapterAdapter.clearPlaying();
        stopUpdatingUI(); // Останавливаем обновление UI
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

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
        if (playbackState == Player.STATE_READY) {
            long durationMs = exoPlayer.getDuration();
            String duration = formatDuration(durationMs);
            txtTotalTime.setText(duration); // Обновление общего времени
            seekBarProgress.setMax((int) durationMs); // Установка максимума SeekBar
            startUpdatingUI(); // Запускаем обновление времени
        }

        if (playbackState == Player.STATE_ENDED) {
            currentPlayingUrl = null;
            if (chapterAdapter != null) chapterAdapter.clearPlaying();
            isPlaying = false;
            updateChapterState("stop"); // Обновляем состояние кнопки на главе
            btnPlayPause.setImageResource(R.drawable.icon_play_circle); // Кнопка Play
            stopUpdatingUI(); // Останавливаем обновление времени
        }
    }

    private void togglePlayPause() {
        if (exoPlayer != null) {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
                updateChapterState("pause"); // Обновляем состояние кнопки на главе
                btnPlayPause.setImageResource(R.drawable.icon_play_circle); // Кнопка Play
                isPlaying = false;
            } else {
                exoPlayer.play();
                updateChapterState("play"); // Обновляем состояние кнопки на главе
                btnPlayPause.setImageResource(R.drawable.icon_pause); // Кнопка Pause
                isPlaying = true;
            }
        }
    }

    @Override
    public void onChapterClick(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) return;

        String directUrl = audioUrl;

        initExoPlayerIfNeeded();

        if (directUrl.equals(currentPlayingUrl)) {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.pause();
                updateChapterState("pause"); // Обновляем состояние кнопки на главе
                stopUpdatingUI(); // Останавливаем обновление времени
                btnPlayPause.setImageResource(R.drawable.icon_play_circle); // Кнопка Play
            } else {
                exoPlayer.play();
                updateChapterState("play"); // Обновляем состояние кнопки на главе
                startUpdatingUI(); // Начинаем обновление времени
                btnPlayPause.setImageResource(R.drawable.icon_pause); // Кнопка Pause
            }
            return;
        }

        playNewAudio(directUrl);
    }

    private void playNewAudio(String directUrl) {
        if (directUrl == null || directUrl.isEmpty()) {
            Toast.makeText(this, "Ссылка на аудиофайл отсутствует.", Toast.LENGTH_SHORT).show();
            return;
        }

        initExoPlayerIfNeeded();

        try {
            if (exoPlayer != null) {
                exoPlayer.stop();
            }

            MediaItem mediaItem = MediaItem.fromUri(directUrl);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();

            exoPlayer.play();
            currentPlayingUrl = directUrl;

            if (chapterAdapter != null) chapterAdapter.setPlayingUrl(directUrl);

            updateChapterState("play"); // Обновляем состояние кнопки на главе
            Toast.makeText(this, "Запускаю воспроизведение...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("BookPage", "Ошибка при подготовке/воспроизведении: " + e.getMessage(), e);
            Toast.makeText(this, "Не удалось воспроизвести файл.", Toast.LENGTH_SHORT).show();
            currentPlayingUrl = null;
            if (chapterAdapter != null) chapterAdapter.clearPlaying();
        }
    }

    private void updateChapterState(String state) {
        // Обновляем состояние кнопки для главы
        this.currentChapterState = state;
        chapterAdapter.updateButtonState(state); // Передаем состояние в адаптер глав
    }

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
            Log.e("BookPage", "Не удалось распарсить Google Drive URL/ID: " + fullUrlOrId, e);
            return fullUrlOrId;
        }
    }

    private long getAudioDuration(String audioUrl) {
        if (durationCache.containsKey(audioUrl)) {
            return durationCache.get(audioUrl);
        }

        long duration = 0;
        try {
            MediaItem mediaItem = MediaItem.fromUri(audioUrl);
            ExoPlayer tempPlayer = new ExoPlayer.Builder(this).build();
            tempPlayer.setMediaItem(mediaItem);
            tempPlayer.prepare();
            duration = tempPlayer.getDuration();
            tempPlayer.release();

            durationCache.put(audioUrl, duration);
        } catch (Exception e) {
            Log.e("BookPage", "Ошибка при получении длительности: " + e.getMessage(), e);
        }
        return duration;
    }

    // Методы для обновления UI

    private void updatePlayerUI() {
        if (exoPlayer != null) {
            long currentPosition = exoPlayer.getCurrentPosition();
            long duration = exoPlayer.getDuration();

            String currentTime = formatDuration(currentPosition);
            String totalTime = formatDuration(duration);

            txtCurrentTime.setText(currentTime); // Обновляем текущее время
            txtTotalTime.setText(totalTime); // Обновляем общее время

            // Обновляем SeekBar
            seekBarProgress.setMax((int) duration);
            seekBarProgress.setProgress((int) currentPosition);
        }
    }

    private void startUpdatingUI() {
        handler.post(updateUIRunnable); // запускаем обновление времени
    }

    private void stopUpdatingUI() {
        handler.removeCallbacks(updateUIRunnable); // останавливаем обновление
    }

    private String formatDuration(long durationMs) {
        if (durationMs < 0) {
            return "00:00";  // Защита от некорректной длительности
        }

        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;

        return String.format("%02d:%02d", minutes, seconds);  // Форматируем "мм:сс"
    }

    private void seekBackward10Sec() {
        if (exoPlayer != null) {
            long currentPosition = exoPlayer.getCurrentPosition();
            long newPosition = Math.max(0, currentPosition - 10000); // на 10 секунд назад
            exoPlayer.seekTo(newPosition);
            updatePlayerUI(); // Обновляем UI после перемотки
        }
    }

    private void seekForward15Sec() {
        if (exoPlayer != null) {
            long currentPosition = exoPlayer.getCurrentPosition();
            long newPosition = Math.min(exoPlayer.getDuration(), currentPosition + 15000); // на 15 секунд вперед
            exoPlayer.seekTo(newPosition);
            updatePlayerUI(); // Обновляем UI после перемотки
        }
    }

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
            cursor = db.query("audioFile", null, selection, selectionArgs, null, null, "filePath ASC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int filePath = cursor.getInt(cursor.getColumnIndexOrThrow("filePath"));
                    String title = "Глава " + filePath;

                    String fileId = cursor.getString(cursor.getColumnIndexOrThrow("sequenceOrder"));
                    String directAudioUrl = convertGoogleDriveUrlToDirect(fileId);

                    long durationMs = getAudioDuration(directAudioUrl);
                    String duration = formatDuration(durationMs);

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
}
