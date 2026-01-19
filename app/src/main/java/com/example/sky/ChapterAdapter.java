package com.example.sky;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    public interface OnChapterClickListener {
        void onChapterClick(int position);
    }

    private final Context context;
    private final List<Chapter> chapterList;
    private final OnChapterClickListener listener;

    private int playingIndex = -1; // индекс активной главы

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
        Chapter chapter = chapterList.get(position);

        holder.titleTextView.setText(chapter.getTitle());
        holder.durationTextView.setText(chapter.getDuration());

        // Подсветка активной главы
        if (position == playingIndex) {
            holder.itemLayout.setBackgroundColor(Color.parseColor("#FFF9C4")); // светло-жёлтый
        } else {
            holder.itemLayout.setBackgroundColor(Color.WHITE);
        }

        // Иконка Play/Pause
        switch (chapter.getState()) {
            case "play":
                holder.playButton.setImageResource(R.drawable.icon_pause);
                break;
            case "pause":
                holder.playButton.setImageResource(R.drawable.icon_play_circle);
                break;
            default:
                holder.playButton.setImageResource(R.drawable.icon_play_circle);
        }

        View.OnClickListener clickHandler = v -> {
            if (listener != null) {
                listener.onChapterClick(holder.getAdapterPosition());
            }
        };

        holder.itemView.setOnClickListener(clickHandler);
        holder.playButton.setOnClickListener(clickHandler);
    }

    public void updateChapterProgress(int chapterIndex, long currentMs) {
        if (chapterIndex < 0 || chapterIndex >= chapterList.size()) return;
        Chapter chapter = chapterList.get(chapterIndex);
        chapter.setDuration(format(currentMs)); // используем форматирование
        notifyItemChanged(chapterIndex);
    }

    // Форматирование времени
    private String format(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    @Override
    public int getItemCount() {
        return chapterList.size();
    }

    /** Установить активную главу и подсветить её */
    public void setPlaying(int position) {
        if (position < 0 || position >= chapterList.size()) return;

        int oldIndex = playingIndex;
        playingIndex = position;

        // Обновляем состояния всех глав
        for (int i = 0; i < chapterList.size(); i++) {
            if (i == playingIndex) {
                chapterList.get(i).setState("play");
            } else {
                chapterList.get(i).setState("stop");
            }
        }

        if (oldIndex >= 0) notifyItemChanged(oldIndex);
        notifyItemChanged(playingIndex);
    }

    /** Поставить на паузу активную главу */
    public void pauseChapter(int position) {
        if (position < 0 || position >= chapterList.size()) return;
        chapterList.get(position).setState("pause");
        notifyItemChanged(position);
    }

    /** Сбросить активную главу */
    public void clearPlaying() {
        if (playingIndex >= 0 && playingIndex < chapterList.size()) {
            chapterList.get(playingIndex).setState("stop");
            notifyItemChanged(playingIndex);
            playingIndex = -1;
        }
    }

    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView, durationTextView;
        public ImageView playButton;
        public LinearLayout itemLayout;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.chapterItemLayout); // контейнер для подсветки
            titleTextView = itemView.findViewById(R.id.textViewChapterTitle);
            durationTextView = itemView.findViewById(R.id.textViewChapterDuration);
            playButton = itemView.findViewById(R.id.btnPlayChapter);
        }
    }
}
