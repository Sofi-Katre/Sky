package com.example.sky;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.sky.Book;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Импорт Glide
import java.util.List;

public class BooksAdapter extends RecyclerView.Adapter<BooksAdapter.BookViewHolder> {

    private Context context;
    private List<Book> books; // Используем класс модели Book

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
        // Используем context из конструктора
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        Glide.with(context) // Используем context для загрузки
                .load(book.imageUrl) // Загружаем из URL, который получили из БД
                .placeholder(R.drawable.zaglushka) // Изображение-заглушка пока грузится
                .error(R.drawable.zaglushka) // Изображение в случае ошибки
                .into(holder.imageViewCover); // Куда грузить

        holder.textViewTitle.setText(book.title);
        holder.textViewAuthor.setText("Автор: " + book.author);
        holder.textViewGenres.setText("Жанр: " + book.genre);
        holder.textViewDate.setText(book.date);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }
}
