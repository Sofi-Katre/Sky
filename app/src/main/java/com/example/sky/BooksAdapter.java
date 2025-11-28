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

    private Context context;
    private List<Book> books;

    public BooksAdapter(Context context, List<Book> books) {
        this.context = context;
        this.books = books;
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCover;
        TextView textViewTitle, textViewAuthor, textViewGenres, textViewDate;

        public BookViewHolder(View itemView) {
            super(itemView);
            imageViewCover = itemView.findViewById(R.id.imageViewCover);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewGenres = itemView.findViewById(R.id.textViewGenres);
            textViewDate = itemView.findViewById(R.id.textViewDate);
        }
    }

    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        String rawImageUrl = book.imageUrl;

        if (rawImageUrl != null && !rawImageUrl.isEmpty()) {
            String directImageUrl = convertGoogleDriveUrlToDirect(rawImageUrl);
            Glide.with(context)
                    .load(directImageUrl)
                    .placeholder(R.drawable.zaglushka)
                    .error(R.drawable.zaglushka)
                    .into(holder.imageViewCover);
        } else {
            holder.imageViewCover.setImageResource(R.drawable.zaglushka);
        }

        holder.textViewTitle.setText(book.title);
        holder.textViewAuthor.setText("Автор: " + book.author);
        holder.textViewGenres.setText("Жанр: " + book.genre);
        holder.textViewDate.setText(book.date);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    // *** ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ПРЕОБРАЗОВАНИЯ URL ***
    private String convertGoogleDriveUrlToDirect(String fullUrl) {
        try {
            int startIndex = fullUrl.indexOf("/d/") + 3;
            int endIndex = fullUrl.indexOf("/view?");

            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                return "https://drive.google.com/uc?export=download&id=" + fullUrl;
            }

            String fileId = fullUrl.substring(startIndex, endIndex);
            return "https://drive.google.com/uc?export=download&id=" + fileId;

        } catch (Exception e) {
            Log.e("BooksAdapter", "Failed to parse Google Drive URL: " + fullUrl, e);
            return fullUrl;
        }
    }
}
