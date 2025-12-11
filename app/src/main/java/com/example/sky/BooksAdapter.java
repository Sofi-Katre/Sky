package com.example.sky;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class BooksAdapter extends RecyclerView.Adapter<BooksAdapter.BookViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }
    private final OnBookClickListener listener;

    private final Context context;
    // !!! ИЗМЕНЕНИЕ 1: Убираем final, чтобы список можно было обновлять !!!
    private List<Book> bookList;

    public BooksAdapter(Context context, List<Book> bookList, OnBookClickListener listener) {
        this.context = context;
        this.bookList = bookList;
        this.listener = listener;
    }

    // !!! ИЗМЕНЕНИЕ 2: Добавляем метод updateList для фильтрации !!!
    public void updateList(List<Book> newList) {
        this.bookList = newList;
        notifyDataSetChanged(); // Говорим RecyclerView перерисовать себя с новыми данными
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book currentBook = bookList.get(position);

        holder.titleTextView.setText(currentBook.title);
        holder.authorTextView.setText("Автор: " + currentBook.author);
        holder.genresTextView.setText("Жанр: " + currentBook.genre);
        holder.dateTextView.setText(currentBook.date);
        holder.iconImageView.setVisibility(View.VISIBLE);

        String directImageUrl = convertGoogleDriveUrlToDirect(currentBook.imageUrl);
        Glide.with(context)
                .load(directImageUrl)
                .placeholder(R.drawable.zaglushka)
                .error(R.drawable.zaglushka)
                .into(holder.coverImageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(currentBook);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    private String convertGoogleDriveUrlToDirect(String fullUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) return "";
        try {
            int startIndex = fullUrl.indexOf("/d/") + 3;
            int endIndex = fullUrl.indexOf("/view?");
            String fileId;
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                fileId = fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
            } else {
                fileId = fullUrl.substring(startIndex, endIndex);
            }
            return "https://drive.google.com/uc?export=download&id=" + fileId;
        } catch (Exception e) {
            Log.e("BooksAdapter", "Failed to parse URL: " + fullUrl, e);
            return fullUrl;
        }
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        public ImageView coverImageView;
        public TextView titleTextView;
        public ImageView iconImageView;
        public TextView authorTextView;
        public TextView genresTextView;
        public TextView dateTextView;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.imageViewCover);
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            iconImageView = itemView.findViewById(R.id.imageViewIcon);
            authorTextView = itemView.findViewById(R.id.textViewAuthor);
            genresTextView = itemView.findViewById(R.id.textViewGenres);
            dateTextView = itemView.findViewById(R.id.textViewDate);
        }
    }
}
