package com.example.sky;

import java.io.Serializable;

public class Book implements Serializable {
    public int bookId;         // Соответствует столбцу bookId (INTEGER)
    public String title;       // Соответствует столбцу book_name (TEXT)
    public String author;      // Соответствует столбцу autore (TEXT)
    public String genre;       // Соответствует столбцу book_genre (TEXT)
    public String date;        // Соответствует столбцу book_date (TEXT)
    public String imageUrl;    // Соответствует столбцу book_image_url (TEXT)

    // ---------------- Добавляем избранное ----------------
    private boolean favorite = false;

    public Book(int bookId, String title, String author, String genre, String date, String imageUrl) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.date = date;
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return bookId;
    }

    // Геттер для избранного
    public boolean isFavorite() {
        return favorite;
    }

    // Сеттер для избранного
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

}