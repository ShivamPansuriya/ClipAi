package com.example.ClipAI.model.video;

public class ImageTiming {
    String imagePath;
    double showTime;
    String keyword;

    public ImageTiming(String imagePath, double showTime, String keyword) {
        this.imagePath = imagePath;
        this.showTime = showTime;
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return String.format("Image: %s at %.2fs for keyword '%s'",
                imagePath, showTime, keyword);
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getShowTime() {
        return showTime;
    }

    public void setShowTime(double showTime) {
        this.showTime = showTime;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}