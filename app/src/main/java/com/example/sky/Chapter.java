package com.example.sky;

import java.io.Serializable;

public class Chapter implements Serializable {
    private String title;
    private String duration;
    private String audioUrl;
    private String state; // "play", "pause", "stop"

    public Chapter(String title, String duration, String audioUrl) {
        this.title = title;
        this.duration = duration;
        this.audioUrl = audioUrl;
        this.state = "stop"; // по умолчанию Stop
    }

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

    public String getState() {
        return state;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public void setState(String state) {
        this.state = state;
    }
}
