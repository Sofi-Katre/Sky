package com.example.sky;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private List<Book> favoriteBooks = new ArrayList<>();
    private final List<Book> allFavoritesOriginal = new ArrayList<>();
    private final Context context;

    private String currentGenreFilter = null;
    private String currentSortMode = "date";

    public FavoritesAdapter(List<Book> books, Context context) {
        this.context = context;
        updateData(books);
    }

    /** ------------------- ОБНОВЛЕНИЕ ДАННЫХ ------------------- */
    public void updateData(List<Book> newList) {
        allFavoritesOriginal.clear();
        allFavoritesOriginal.addAll(newList);

        applyFilters(currentGenreFilter, currentSortMode);
    }

    /** ------------------- ФИЛЬТР + СОРТИРОВКА ------------------- */
    public void applyFilters(String genre, String sortMode) {
        this.currentGenreFilter = genre;
        this.currentSortMode = sortMode;

        List<Book> filteredList = new ArrayList<>();

        // Фильтр
        for (Book book : allFavoritesOriginal) {
            boolean matchesGenre = (genre == null ||
                    book.genre.equalsIgnoreCase(genre));

            if (matchesGenre) {
                filteredList.add(book);
            }
        }

        // Сортировка
        if ("alpha".equals(sortMode)) {
            Collections.sort(filteredList,
                    (b1, b2) -> b1.title.compareToIgnoreCase(b2.title));
        } else {
            Collections.sort(filteredList,
                    (b1, b2) -> Integer.compare(b2.bookId, b1.bookId));
        }

        favoriteBooks = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_book, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Book book = favoriteBooks.get(position);

        holder.title.setText(book.title);
        holder.author.setText("Автор: " + book.author);

        if (holder.date != null) {
            holder.date.setText("Добавлено: " + book.date);
        }

        if (holder.genres != null) {
            holder.genres.setText("Жанр: " + book.genre);
        }

        // Иконка избранного
        holder.icon.setImageResource(R.drawable.zakladkaon);
        book.setFavorite(true);

        holder.icon.setOnClickListener(v -> removeFromFavorites(holder.getAdapterPosition()));

        // Картинка
        Glide.with(context)
                .load(convertGoogleDriveUrlToDirect(book.imageUrl))
                .placeholder(R.drawable.zaglushka)
                .error(R.drawable.zaglushka)
                .into(holder.cover);

        // Переход
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookPage.class);
            intent.putExtra("SELECTED_BOOK", book);
            context.startActivity(intent);
        });
    }

    /** ------------------- УДАЛЕНИЕ ИЗ ИЗБРАННОГО ------------------- */
    private void removeFromFavorites(int position) {
        if (position == RecyclerView.NO_POSITION) return;

        Book bookToRemove = favoriteBooks.get(position);

        // Удаляем из оригинального списка
        allFavoritesOriginal.remove(bookToRemove);

        // Firebase
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            FirebaseFirestore.getInstance()
                    .collection("favorites")
                    .document(userId)
                    .update(String.valueOf(bookToRemove.bookId), FieldValue.delete())
                    .addOnFailureListener(e -> {
                        // Возврат при ошибке
                        allFavoritesOriginal.add(bookToRemove);
                        applyFilters(currentGenreFilter, currentSortMode);
                        Toast.makeText(context, "Ошибка синхронизации!", Toast.LENGTH_SHORT).show();
                    });
        }

        // Обновляем список с учетом фильтров
        applyFilters(currentGenreFilter, currentSortMode);
    }

    /** ------------------- GOOGLE DRIVE FIX ------------------- */
    private String convertGoogleDriveUrlToDirect(String fullUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) return "";

        if (!fullUrl.contains("http")) {
            return "https://drive.google.com/uc?export=download&id=" + fullUrl;
        }

        try {
            int startIndex = fullUrl.indexOf("/d/");
            int endIndex = fullUrl.indexOf("/view");

            if (startIndex != -1 && endIndex != -1) {
                startIndex += 3;
                String fileId = fullUrl.substring(startIndex, endIndex);
                return "https://drive.google.com/uc?export=download&id=" + fileId;
            }

            return fullUrl;

        } catch (Exception e) {
            return fullUrl;
        }
    }

    @Override
    public int getItemCount() {
        return favoriteBooks.size();
    }

    /** ------------------- VIEWHOLDER ------------------- */
    static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        ImageView cover, icon;
        TextView title, author, date, genres;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.imageViewCover);
            icon = itemView.findViewById(R.id.imageViewIcon);
            title = itemView.findViewById(R.id.textViewTitle);
            author = itemView.findViewById(R.id.textViewAuthor);
            date = itemView.findViewById(R.id.textViewDate);
            genres = itemView.findViewById(R.id.textViewGenres);
        }
    }
}