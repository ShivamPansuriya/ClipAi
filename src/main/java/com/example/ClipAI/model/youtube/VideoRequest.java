package com.example.ClipAI.model.youtube;

import java.util.List;

public class VideoRequest {
    private String videoId;

    private String title;

    private String description;

    private List<String> tags;

    private String privacyStatus;

    private String videoPath;

    private int uploadDelay;        // time in minutes

    public int getUploadDelay() {
        return uploadDelay;
    }

    public void setUploadDelay(int uploadDelay) {
        this.uploadDelay = uploadDelay;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getPrivacyStatus() {
        return privacyStatus;
    }

    public void setPrivacyStatus(String privacyStatus) {
        this.privacyStatus = privacyStatus;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
}