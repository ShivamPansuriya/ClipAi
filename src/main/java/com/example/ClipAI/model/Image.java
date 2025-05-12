package com.example.ClipAI.model;

/**
 * Represents an image to be generated or used in the video.
 */
public class Image {
    private String key;
    private String description;
    private int width;
    private int height;

    /**
     * Gets the key.
     *
     * @return The key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key.
     *
     * @param key The key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the width.
     *
     * @return The width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width.
     *
     * @param width The width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the height.
     *
     * @return The height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height.
     *
     * @param height The height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }
}
