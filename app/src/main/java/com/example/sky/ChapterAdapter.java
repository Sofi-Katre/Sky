package com.example.sky;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    public interface OnChapterClickListener {
        void onChapterClick(String audioUrl);
    }

    private final Context context;
    private final List<Chapter> chapterList;
    private final OnChapterClickListener listener;

    private String playingUrl = null;

    public ChapterAdapter(Context context, List<Chapter> chapterList, OnChapterClickListener listener) {
        this.context = context;
        this.chapterList = chapterList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter currentChapter = chapterList.get(position);
        holder.titleTextView.setText(currentChapter.getTitle());
        holder.durationTextView.setText(currentChapter.getDuration());  // Отображаем длительность

        // Изменяем состояние кнопки в зависимости от текущего состояния главы
        if (currentChapter.getState().equals("play")) {
            holder.playButton.setImageResource(R.drawable.icon_pause); // Если играет
        } else if (currentChapter.getState().equals("pause")) {
            holder.playButton.setImageResource(R.drawable.icon_play_circle); // Если на паузе
        } else {
            holder.playButton.setImageResource(R.drawable.icon_play_circle); // Иначе, если стоп
        }

        View.OnClickListener clickHandler = v -> {
            if (listener != null && currentChapter.getAudioUrl() != null) {
                listener.onChapterClick(currentChapter.getAudioUrl());
            }
        };

        holder.itemView.setOnClickListener(clickHandler);
        holder.playButton.setOnClickListener(clickHandler);
    }

    @Override
    public int getItemCount() {
        return chapterList != null ? chapterList.size() : 0;
    }

    public void setPlayingUrl(String url) {
        if (url == null) return;
        if (url.equals(this.playingUrl)) return;

        int oldPos = indexOfUrl(this.playingUrl);
        int newPos = indexOfUrl(url);

        this.playingUrl = url;

        if (oldPos >= 0) notifyItemChanged(oldPos);
        if (newPos >= 0) notifyItemChanged(newPos);
    }

    public void clearPlaying() {
        if (this.playingUrl == null) return;
        int oldPos = indexOfUrl(this.playingUrl);
        this.playingUrl = null;
        if (oldPos >= 0) notifyItemChanged(oldPos);
    }

    private int indexOfUrl(String url) {
        if (url == null) return -1;
        for (int i = 0; i < chapterList.size(); i++) {
            Chapter c = chapterList.get(i);
            if (url.equals(c.getAudioUrl())) return i;
        }
        return -1;
    }

    // Метод для обновления состояния кнопки
    public void updateButtonState(String state) {
        for (int i = 0; i < chapterList.size(); i++) {
            Chapter chapter = chapterList.get(i);
            // Устанавливаем состояние кнопки для каждой главы
            if (playingUrl != null && playingUrl.equals(chapter.getAudioUrl())) {
                if (state.equals("pause")) {
                    chapter.setState("pause");
                } else if (state.equals("play")) {
                    chapter.setState("play");
                } else if (state.equals("stop")) {
                    chapter.setState("stop");
                }
            }
        }
        notifyDataSetChanged();
    }


    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView durationTextView;  // Для отображения длительности
        public ImageView playButton;

        public ChapterViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewChapterTitle);
            durationTextView = itemView.findViewById(R.id.textViewChapterDuration);  // Инициализируем для длительности
            playButton = itemView.findViewById(R.id.btnPlayChapter);
        }
    }
}
