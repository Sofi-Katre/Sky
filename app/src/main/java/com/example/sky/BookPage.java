package com.example.sky;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.AudioAttributes;

import java.util.ArrayList;
import java.util.List;

public class BookPage extends AppCompatActivity
        implements ChapterAdapter.OnChapterClickListener, Player.Listener {

    private DatabaseHelper dbHelper;
    private Book currentBook;

    private List<Chapter> chapterList = new ArrayList<>();
    private List<MediaItem> mediaItemList = new ArrayList<>();

    private ExoPlayer exoPlayer;
    private int currentChapterIndex = -1;

    private RecyclerView recyclerViewChapters;
    private ChapterAdapter chapterAdapter;

    private TextView txtCurrentTime, txtTotalTime;
    private SeekBar seekBarProgress;
    private ImageView btnPlayPause, btnRewind10, btnForward15;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_page);

        dbHelper = new DatabaseHelper(this);

        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnRewind10 = findViewById(R.id.btnRewind10);
        btnForward15 = findViewById(R.id.btnForward15);

        btnPlayPause.setImageResource(R.drawable.icon_play_circle); // по умолчанию Stop

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnRewind10.setOnClickListener(v -> seekBy(-10000));
        btnForward15.setOnClickListener(v -> seekBy(15000));

        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && exoPlayer != null) {
                    txtCurrentTime.setText(format(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (exoPlayer != null) {
                    exoPlayer.seekTo(seekBar.getProgress());
                    handler.post(updateRunnable);
                }
            }
        });

        currentBook = (Book) getIntent().getSerializableExtra("SELECTED_BOOK");
        if (currentBook == null) {
            finish();
            return;
        }

        setupBookInfo();
        setupChapterList(currentBook.bookId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initPlayer();

        // Восстанавливаем позицию после инициализации плеера
        if (!mediaItemList.isEmpty()) {
            restorePlaybackPosition();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePlaybackPosition();
        handler.removeCallbacks(updateRunnable);
        PlayerManager.releasePlayer();
        exoPlayer = null;
    }

    private void initPlayer() {
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
            MediaItem item = MediaItem.fromUri(chapter.getAudioUrl());
            mediaItemList.add(item);
            // Получаем реальную длительность
            fetchDurationAsync(item, chapter);
        }
    }

    private List<Chapter> loadChapters(int bookId) {
        List<Chapter> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getDatabase();

        Cursor cursor = db.query(
                "audioFile",
                null,
                "bookId = ?",
                new String[]{String.valueOf(bookId)},
                null, null,
                "filePath ASC"
        );

        int chapterNumber = 1;

        while (cursor.moveToNext()) {
            String fileId = cursor.getString(cursor.getColumnIndexOrThrow("sequenceOrder"));
            String url = convertGoogleDriveUrlToDirect(fileId);

            list.add(new Chapter(
                    "Глава " + chapterNumber,
                    "00:00", // временно, потом заменим на реальную
                    url
            ));

            chapterNumber++;
        }

        cursor.close();
        return list;
    }

    // Получаем реальную длительность через временный ExoPlayer
    private void fetchDurationAsync(MediaItem item, Chapter chapter) {
        ExoPlayer tempPlayer = new ExoPlayer.Builder(this).build();
        tempPlayer.setMediaItem(item);
        tempPlayer.prepare();
        tempPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long durationMs = tempPlayer.getDuration();
                    chapter.setDuration(format(durationMs));
                    chapterAdapter.notifyDataSetChanged();
                    tempPlayer.release();
                }
            }
        });
    }

    @Override
    public void onChapterClick(int position) {
        if (position < 0 || position >= mediaItemList.size()) return;

        if (currentChapterIndex == position) {
            // Play/Pause текущей главы
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
                chapterAdapter.pauseChapter(position);
                btnPlayPause.setImageResource(R.drawable.icon_play_circle);
            } else {
                exoPlayer.play();
                chapterAdapter.setPlaying(position);
                btnPlayPause.setImageResource(R.drawable.icon_pause);
            }
        } else {
            // Воспроизвести новую главу
            playFromIndex(position);
        }
    }

    private void playFromIndex(int index) {
        if (index < 0 || index >= mediaItemList.size()) return;

        exoPlayer.stop();
        exoPlayer.setMediaItems(mediaItemList, index, 0);
        exoPlayer.prepare();
        exoPlayer.play();

        currentChapterIndex = index;

        chapterAdapter.setPlaying(index); // подсветка и кнопка
        btnPlayPause.setImageResource(R.drawable.icon_pause);
        handler.post(updateRunnable);
    }

    private void togglePlayPause() {
        if (exoPlayer == null || currentChapterIndex < 0) return;

        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
            chapterAdapter.pauseChapter(currentChapterIndex);
            btnPlayPause.setImageResource(R.drawable.icon_play_circle);
        } else {
            exoPlayer.play();
            chapterAdapter.setPlaying(currentChapterIndex);
            btnPlayPause.setImageResource(R.drawable.icon_pause);
        }
    }

    private void seekBy(long ms) {
        if (exoPlayer == null) return;
        long pos = Math.max(0, exoPlayer.getCurrentPosition() + ms);
        exoPlayer.seekTo(pos);
    }

    private void updatePlayerUI() {
        if (exoPlayer == null) return;

        long duration = exoPlayer.getDuration();
        if (duration <= 0) return;

        txtCurrentTime.setText(format(exoPlayer.getCurrentPosition()));
        txtTotalTime.setText(format(duration));

        seekBarProgress.setMax((int) duration);
        seekBarProgress.setProgress((int) exoPlayer.getCurrentPosition());
    }

    private int findIndexByUrl(String url) {
        for (int i = 0; i < chapterList.size(); i++) {
            if (chapterList.get(i).getAudioUrl().equals(url)) return i;
        }
        return -1;
    }

    private void savePlaybackPosition() {
        if (exoPlayer == null || currentChapterIndex < 0) return;

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_BOOK + currentBook.bookId + "_chapter", currentChapterIndex)
                .putLong(KEY_BOOK + currentBook.bookId + "_position", exoPlayer.getCurrentPosition())
                .apply();
    }

    private void restorePlaybackPosition() {
        if (exoPlayer == null || mediaItemList.isEmpty()) return;

        int chapter = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_BOOK + currentBook.bookId + "_chapter", -1);

        long position = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getLong(KEY_BOOK + currentBook.bookId + "_position", 0);

        if (chapter >= 0 && chapter < mediaItemList.size()) {
            exoPlayer.setMediaItems(mediaItemList, chapter, position);
            exoPlayer.prepare();
            exoPlayer.pause();

            currentChapterIndex = chapter;
            chapterAdapter.setPlaying(chapter); // подсветка и кнопка
            updatePlayerUI();
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        currentChapterIndex = exoPlayer.getCurrentMediaItemIndex();
        if (currentChapterIndex >= 0 && currentChapterIndex < chapterList.size()) {
            chapterAdapter.setPlaying(currentChapterIndex);
        }
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        if (state == Player.STATE_ENDED) {
            int nextIndex = currentChapterIndex + 1;
            if (nextIndex < mediaItemList.size()) {
                playFromIndex(nextIndex);
            } else {
                btnPlayPause.setImageResource(R.drawable.icon_play_circle);
                chapterAdapter.clearPlaying();
                handler.removeCallbacks(updateRunnable);
            }
        }
    }

    private String format(long ms) {
        if (ms < 0) return "00:00";
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private String convertGoogleDriveUrlToDirect(String id) {
        return "https://drive.google.com/uc?export=download&id=" + id;
    }
}
