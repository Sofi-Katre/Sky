package com.example.sky;

import java.io.Serializable;

public class Chapter implements Serializable {
    private String title;
    private String duration;
    private String audioUrl;

    public Chapter(String title, String duration, String audioUrl) {
        this.title = title;
        this.duration = duration;
        this.audioUrl = audioUrl;
    }

    // Пустой конструктор нужен, если вы используете десериализацию/ORM/JSON
    public Chapter() {}

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    // Опционально: сеттеры, если данные меняются
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}
