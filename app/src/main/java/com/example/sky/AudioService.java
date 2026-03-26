package com.example.sky;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

public class AudioService extends Service {
    private final IBinder binder = new LocalBinder();
    public ExoPlayer player;
    private PlayerNotificationManager notificationManager;

    public class LocalBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();

        // Настройка уведомления (Пункт 5 твоего плана)
        notificationManager = new PlayerNotificationManager.Builder(
                this,
                101, // ID уведомления
                "sky_audio_channel" // ID канала
        )
                .setChannelNameResourceId(R.string.app_name)
                .setMediaDescriptionAdapter(new DescriptionAdapter())
                .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        if (ongoing) {
                            startForeground(notificationId, notification);
                        }
                    }
                })
                .build();

        notificationManager.setPlayer(player);
    }

    // Адаптер для текста в уведомлении (Название книги/главы)
    private class DescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

        @Nullable
        @Override
        public CharSequence getCurrentContentText(Player player) {
            return "Аудиокнига";
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            return null; // Здесь будет обложка книги
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            // Теперь при клике на уведомление откроется экран плеера (Music.class)
            Intent intent = new Intent(AudioService.this, Music.class);
            return PendingIntent.getActivity(
                    AudioService.this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }

        @Override
        public CharSequence getCurrentContentTitle(Player player) {
            // Если в плеер переданы MediaMetadata, заголовок подтянется сам
            return player.getMediaMetadata().title != null ? player.getMediaMetadata().title : "Sky Audio";
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        notificationManager.setPlayer(null);
        player.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
