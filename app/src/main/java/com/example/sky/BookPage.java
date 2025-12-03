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
import android.widget.Button;
import com.example.sky.PlayerManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AudioAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookPage extends AppCompatActivity implements ChapterAdapter.OnChapterClickListener, Player.Listener {

    private RecyclerView recyclerViewChapters;
    private ChapterAdapter chapterAdapter;
    private List<Chapter> chapterList;
    private DatabaseHelper dbHelper;
    private Book currentBook;

    // Плеер
    private ExoPlayer exoPlayer;

    // UI элементы книги
    private TextView titleBookTextView, authorTextView, genreTextView, dateTextView;
    private ImageView coverImageView;
    private Button btnBack;

    // UI элементы плеера
    private TextView txtCurrentTime, txtTotalTime;
    private SeekBar seekBarProgress;
    private ImageView btnPlayPause, btnRewind10, btnForward15;

    // Длительности аудиофайлов
    private Map<String, Long> durationCache = new HashMap<>();

    // Текущий воспроизводимый URL
    private String currentPlayingUrl = null;

    // Состояние воспроизведения
    private boolean isPlaying = false;
    private String currentChapterState = "stop"; // "stop", "play", "pause"

    // Handler для обновления UI
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateUIRunnable = new Runnable() {
        @Override
        public void run() {
            updatePlayerUI();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_page);

        dbHelper = new DatabaseHelper(this);

        // Инициализация UI плеера
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind10 = findViewById(R.id.btnRewind10);
        btnForward15 = findViewById(R.id.btnForward15);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnRewind10.setOnClickListener(v -> seekBackward10Sec());
        btnForward15.setOnClickListener(v -> seekForward15Sec());

        // Получаем выбранную книгу
        currentBook = (Book) getIntent().getSerializableExtra("SELECTED_BOOK");
        if (currentBook != null) {
            setupBookDetailsUI(currentBook);
            setupChapterList(currentBook.bookId);
        } else {
            Toast.makeText(this, "Ошибка: данные о книге не найдены", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initExoPlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopUpdatingUI();
        if (chapterAdapter != null) chapterAdapter.clearPlaying();

        // Вариант Б: полностью освобождаем плеер при выходе
        PlayerManager.releasePlayer();
        exoPlayer = null;
        currentPlayingUrl = null;
        isPlaying = false;
        currentChapterState = "stop";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    private void initExoPlayer() {
        if (exoPlayer == null) {
            exoPlayer = PlayerManager.getPlayer(this);
            exoPlayer.addListener(this);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build();
            exoPlayer.setAudioAttributes(audioAttributes, true);
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

    private void setupChapterList(int bookId) {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);
        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));
        chapterList = getChaptersFromDB(bookId);
        chapterAdapter = new ChapterAdapter(this, chapterList, this);
        recyclerViewChapters.setAdapter(chapterAdapter);
    }

    @Override
    public void onChapterClick(String audioUrl) {
        if (audioUrl == null || audioUrl.isEmpty()) return;

        String directUrl = audioUrl;
        initExoPlayer();

        if (directUrl.equals(currentPlayingUrl)) {
            togglePlayPause();
            return;
        }

        playNewAudio(directUrl);
    }

    private void playNewAudio(String directUrl) {
        if (directUrl == null || directUrl.isEmpty()) {
            Toast.makeText(this, "Ссылка на аудиофайл отсутствует.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (exoPlayer != null) exoPlayer.stop();

            MediaItem mediaItem = MediaItem.fromUri(directUrl);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();

            currentPlayingUrl = directUrl;
            if (chapterAdapter != null) chapterAdapter.setPlayingUrl(directUrl);

            updateChapterState("play");
        } catch (Exception e) {
            Log.e("BookPage", "Ошибка воспроизведения: " + e.getMessage(), e);
            Toast.makeText(this, "Не удалось воспроизвести файл.", Toast.LENGTH_SHORT).show();
            currentPlayingUrl = null;
            if (chapterAdapter != null) chapterAdapter.clearPlaying();
        }
    }

    private void togglePlayPause() {
        if (exoPlayer == null) return;

        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
            updateChapterState("pause");
            btnPlayPause.setImageResource(R.drawable.icon_play_circle);
            stopUpdatingUI();
            isPlaying = false;
        } else {
            exoPlayer.play();
            updateChapterState("play");
            btnPlayPause.setImageResource(R.drawable.icon_pause);
            startUpdatingUI();
            isPlaying = true;
        }
    }

    private void updateChapterState(String state) {
        this.currentChapterState = state;
        if (chapterAdapter != null) chapterAdapter.updateButtonState(state);
    }

    private void startUpdatingUI() {
        handler.post(updateUIRunnable);
    }

    private void stopUpdatingUI() {
        handler.removeCallbacks(updateUIRunnable);
    }

    private void updatePlayerUI() {
        if (exoPlayer == null) return;

        long currentPosition = exoPlayer.getCurrentPosition();
        long duration = exoPlayer.getDuration();

        txtCurrentTime.setText(formatDuration(currentPosition));
        txtTotalTime.setText(formatDuration(duration));

        seekBarProgress.setMax((int) duration);
        seekBarProgress.setProgress((int) currentPosition);
    }

    private void seekBackward10Sec() {
        if (exoPlayer == null) return;
        long pos = Math.max(0, exoPlayer.getCurrentPosition() - 10000);
        exoPlayer.seekTo(pos);
        updatePlayerUI();
    }

    private void seekForward15Sec() {
        if (exoPlayer == null) return;
        long pos = Math.min(exoPlayer.getDuration(), exoPlayer.getCurrentPosition() + 15000);
        exoPlayer.seekTo(pos);
        updatePlayerUI();
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_READY) {
            startUpdatingUI();
        }
        if (playbackState == Player.STATE_ENDED) {
            currentPlayingUrl = null;
            if (chapterAdapter != null) chapterAdapter.clearPlaying();
            isPlaying = false;
            updateChapterState("stop");
            btnPlayPause.setImageResource(R.drawable.icon_play_circle);
            stopUpdatingUI();
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        Toast.makeText(this, "Ошибка воспроизведения.", Toast.LENGTH_SHORT).show();
        Log.e("BookPage", "ExoPlayer error: " + (error != null ? error.getMessage() : "null"));
        currentPlayingUrl = null;
        if (chapterAdapter != null) chapterAdapter.clearPlaying();
    }

    // Работа с базой данных
    private List<Chapter> getChaptersFromDB(int bookId) {
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
            while (cursor != null && cursor.moveToNext()) {
                int filePath = cursor.getInt(cursor.getColumnIndexOrThrow("filePath"));
                String title = "Глава " + filePath;

                String fileId = cursor.getString(cursor.getColumnIndexOrThrow("sequenceOrder"));
                String directAudioUrl = convertGoogleDriveUrlToDirect(fileId);

                long durationMs = getAudioDuration(directAudioUrl);
                String duration = formatDuration(durationMs);

                list.add(new Chapter(title, duration, directAudioUrl));
            }
        } catch (Exception e) {
            Log.e("BookPage", "Ошибка чтения из БД: " + e.getMessage(), e);
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }

        return list;
    }

    private long getAudioDuration(String audioUrl) {
        if (durationCache.containsKey(audioUrl)) return durationCache.get(audioUrl);
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

    private String formatDuration(long durationMs) {
        if (durationMs < 0) return "00:00";
        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String convertGoogleDriveUrlToDirect(String fullUrlOrId) {
        if (fullUrlOrId == null || fullUrlOrId.isEmpty()) return "";
        try {
            String fileId = fullUrlOrId;
            if (fullUrlOrId.contains("/d/")) {
                int start = fullUrlOrId.indexOf("/d/") + 3;
                int end = fullUrlOrId.indexOf('/', start);
                if (end == -1) end = fullUrlOrId.length();
                fileId = fullUrlOrId.substring(start, end);
            } else if (fullUrlOrId.contains("open?id=")) {
                int idx = fullUrlOrId.indexOf("open?id=") + 8;
                fileId = fullUrlOrId.substring(idx);
            } else if (fullUrlOrId.contains("id=") && fullUrlOrId.contains("drive.google.com")) {
                int idx = fullUrlOrId.indexOf("id=") + 3;
                fileId = fullUrlOrId.substring(idx);
            } else if (fullUrlOrId.startsWith("http") && fullUrlOrId.contains("drive.google.com")) {
                fileId = fullUrlOrId.substring(fullUrlOrId.lastIndexOf('/') + 1);
            }
            return "https://drive.google.com/uc?export=download&id=" + fileId;
        } catch (Exception e) {
            Log.e("BookPage", "Не удалось распарсить URL: " + fullUrlOrId, e);
            return fullUrlOrId;
        }
    }
}
