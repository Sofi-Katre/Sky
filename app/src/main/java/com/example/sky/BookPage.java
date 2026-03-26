package com.example.sky;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;

public class BookPage extends AppCompatActivity
        implements ChapterAdapter.OnChapterClickListener, Player.Listener {

    private DatabaseHelper dbHelper;
    private Book currentBook;
    private List<Chapter> chapterList = new ArrayList<>();
    private List<MediaItem> mediaItemList = new ArrayList<>();

    // Поля для связи с AudioService
    private AudioService audioService;
    private boolean isBound = false;
    private ExoPlayer exoPlayer;

    private int currentChapterIndex = -1;
    private RecyclerView recyclerViewChapters;
    private ChapterAdapter chapterAdapter;

    private TextView txtCurrentTime, txtTotalTime;
    private SeekBar seekBarProgress;
    private ImageView btnPlayPause, btnRewind10, btnForward15, btnFavorite;

    private static final String PREFS_NAME = "audio_progress";
    private static final String KEY_BOOK = "book_";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updatePlayerUI();
            handler.postDelayed(this, 1000);
        }
    };

    // Слушатель подключения к сервису
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
            audioService = binder.getService();
            exoPlayer = audioService.player;
            exoPlayer.addListener(BookPage.this);
            isBound = true;

            // Если сервис уже что-то играет, подтягиваем данные в UI
            if (exoPlayer.getPlaybackState() == Player.STATE_READY || exoPlayer.isPlaying()) {
                restoreUIFromService();
            } else {
                restorePlaybackPosition();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_page);

        dbHelper = new DatabaseHelper(this);
        initViews();

        currentBook = (Book) getIntent().getSerializableExtra("SELECTED_BOOK");
        if (currentBook == null) { finish(); return; }

        setupBookInfo();
        setupChapterList(currentBook.bookId);

        // Запускаем сервис сразу, чтобы он не умер при выходе из Activity
        Intent intent = new Intent(this, AudioService.class);
        startService(intent);
    }

    private void initViews() {
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind10 = findViewById(R.id.btnRewind10);
        btnForward15 = findViewById(R.id.btnForward15);
        btnFavorite = findViewById(R.id.btnWhether);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnRewind10.setOnClickListener(v -> seekBy(-10000));
        btnForward15.setOnClickListener(v -> seekBy(15000));
        if (btnFavorite != null) btnFavorite.setOnClickListener(v -> toggleFavorite());

        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) txtCurrentTime.setText(format(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { handler.removeCallbacks(updateRunnable); }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (exoPlayer != null) {
                    exoPlayer.seekTo(seekBar.getProgress());
                    handler.post(updateRunnable);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, AudioService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePlaybackPosition();
        if (isBound) {
            exoPlayer.removeListener(this);
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacks(updateRunnable);
    }

    private void restoreUIFromService() {
        currentChapterIndex = exoPlayer.getCurrentMediaItemIndex();
        chapterAdapter.setPlaying(currentChapterIndex);
        btnPlayPause.setImageResource(exoPlayer.isPlaying() ? R.drawable.icon_pause : R.drawable.icon_play_circle);
        handler.post(updateRunnable);
    }

    private void setupBookInfo() {
        ((TextView) findViewById(R.id.titleBook)).setText(currentBook.title);
        ((TextView) findViewById(R.id.txtAutore)).setText("Автор: " + currentBook.author);
        ((TextView) findViewById(R.id.txtJaner)).setText("Жанр: " + currentBook.genre);
        ((TextView) findViewById(R.id.txtDate)).setText("Добавлено: " + currentBook.date);

        Glide.with(this)
                .load(convertGoogleDriveUrlToDirect(currentBook.imageUrl))
                .placeholder(R.drawable.zaglushka)
                .into((ImageView) findViewById(R.id.imageView11));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupChapterList(int bookId) {
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);
        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));
        chapterList = loadChapters(bookId);
        chapterAdapter = new ChapterAdapter(this, chapterList, this);
        recyclerViewChapters.setAdapter(chapterAdapter);

        mediaItemList.clear();
        for (Chapter chapter : chapterList) {
            mediaItemList.add(MediaItem.fromUri(chapter.getAudioUrl()));
        }
    }

    private List<Chapter> loadChapters(int bookId) {
        List<Chapter> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getDatabase();
        Cursor cursor = db.query("audioFile", null, "bookId = ?",
                new String[]{String.valueOf(bookId)}, null, null, "filePath ASC");

        int chapterNumber = 1;
        while (cursor.moveToNext()) {
            String fileId = cursor.getString(cursor.getColumnIndexOrThrow("sequenceOrder"));
            list.add(new Chapter("Глава " + chapterNumber, "0:00", convertGoogleDriveUrlToDirect(fileId)));
            chapterNumber++;
        }
        cursor.close();
        return list;
    }

    @Override
    public void onChapterClick(int position) {
        if (!isBound || position < 0 || position >= mediaItemList.size()) return;

        if (currentChapterIndex == position) {
            togglePlayPause();
        } else {
            playFromIndex(position);
        }
    }

    private void playFromIndex(int index) {
        exoPlayer.stop();
        exoPlayer.setMediaItems(mediaItemList, index, 0);
        exoPlayer.prepare();
        exoPlayer.play();
        currentChapterIndex = index;
        chapterAdapter.setPlaying(index);
        btnPlayPause.setImageResource(R.drawable.icon_pause);
        handler.post(updateRunnable);
    }

    private void togglePlayPause() {
        if (exoPlayer == null) return;
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.icon_play_circle);
            chapterAdapter.pauseChapter(currentChapterIndex);
        } else {
            if (currentChapterIndex == -1) playFromIndex(0);
            else {
                exoPlayer.play();
                btnPlayPause.setImageResource(R.drawable.icon_pause);
                chapterAdapter.setPlaying(currentChapterIndex);
            }
        }
    }

    private void seekBy(long ms) {
        if (exoPlayer != null) exoPlayer.seekTo(Math.max(0, exoPlayer.getCurrentPosition() + ms));
    }

    private void updatePlayerUI() {
        if (exoPlayer == null || !exoPlayer.isPlaying() && exoPlayer.getPlaybackState() != Player.STATE_READY) return;
        long duration = exoPlayer.getDuration();
        if (duration > 0) {
            txtCurrentTime.setText(format(exoPlayer.getCurrentPosition()));
            txtTotalTime.setText(format(duration));
            seekBarProgress.setMax((int) duration);
            seekBarProgress.setProgress((int) exoPlayer.getCurrentPosition());
        }
    }

    private void toggleFavorite() {
        // Логика для Пункта 4: Добавление в Room
        Toast.makeText(this, "Книга " + currentBook.title + " в избранном!", Toast.LENGTH_SHORT).show();
    }

    private void savePlaybackPosition() {
        if (exoPlayer == null || currentChapterIndex < 0) return;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_BOOK + currentBook.bookId + "_chapter", currentChapterIndex)
                .putLong(KEY_BOOK + currentBook.bookId + "_position", exoPlayer.getCurrentPosition())
                .apply();
    }

    private void restorePlaybackPosition() {
        int chapter = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_BOOK + currentBook.bookId + "_chapter", -1);
        long position = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getLong(KEY_BOOK + currentBook.bookId + "_position", 0);
        if (chapter >= 0 && chapter < mediaItemList.size()) {
            exoPlayer.setMediaItems(mediaItemList, chapter, position);
            exoPlayer.prepare();
            currentChapterIndex = chapter;
            chapterAdapter.setPlaying(chapter);
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        currentChapterIndex = exoPlayer.getCurrentMediaItemIndex();
        chapterAdapter.setPlaying(currentChapterIndex);
    }

    private String format(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private String convertGoogleDriveUrlToDirect(String id) {
        return "https://drive.google.com/uc?export=download&id=" + id;
    }
}
