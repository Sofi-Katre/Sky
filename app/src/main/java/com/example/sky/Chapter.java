package com.example.sky;

public class Chapter {
    private String title;
    private String duration;

    public Chapter(String title, String duration) {
        this.title = title;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }
}
