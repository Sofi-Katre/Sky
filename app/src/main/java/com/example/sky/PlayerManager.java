package com.example.sky;

import android.content.Context;
import com.google.android.exoplayer2.ExoPlayer;

public class PlayerManager {

    private static ExoPlayer exoPlayer = null;

    public static ExoPlayer getPlayer(Context context) {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(context.getApplicationContext()).build();
        }
        return exoPlayer;
    }

    public static void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
