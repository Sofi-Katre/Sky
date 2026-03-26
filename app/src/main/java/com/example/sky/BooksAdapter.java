package com.example.sky;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BooksAdapter extends RecyclerView.Adapter<BooksAdapter.BookViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Book book);
        void onFavoriteClick(Book book);
    }

    private final OnBookClickListener listener;
    private final Context context;
    private List<Book> bookList;

    public BooksAdapter(Context context, List<Book> bookList, OnBookClickListener listener) {
        this.context = context;
        this.bookList = bookList;
        this.listener = listener;
    }

    public void updateList(List<Book> newList) {
        this.bookList = newList;
        notifyDataSetChanged();
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

        // Иконка теперь всегда берет актуальный статус из объекта Book
        holder.iconImageView.setImageResource(currentBook.isFavorite() ? R.drawable.zakladkaon : R.drawable.zakladkaoff);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBookClick(currentBook);
        });

        // --- ИСПРАВЛЕНИЕ: Отправляем клик в Books.java, где живет логика Firebase ---
        holder.iconImageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(currentBook);
            }
        });

        String directImageUrl = convertGoogleDriveUrlToDirect(currentBook.imageUrl);
        Glide.with(context)
                .load(directImageUrl)
                .placeholder(R.drawable.zaglushka)
                .error(R.drawable.zaglushka)
                .into(holder.coverImageView);
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

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;
        ImageView iconImageView;
        TextView authorTextView, genresTextView, dateTextView;

        BookViewHolder(@NonNull View itemView) {
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