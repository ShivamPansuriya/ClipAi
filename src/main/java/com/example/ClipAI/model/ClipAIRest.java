package com.example.ClipAI.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Main data transfer object for the ClipAI application.
 * Contains information about the video to be generated.
 */
public class ClipAIRest {
    private String topic;
    private String script;
    private List<Image> images;
    private int imageWidth;
    private int imageHeight;

    /**
     * Gets the topic.
     *
     * @return The topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets the topic.
     *
     * @param topic The topic to set
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Gets the script.
     *
     * @return The script
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the script.
     *
     * @param script The script to set
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Gets the images.
     *
     * @return The images
     */
    public List<Image> getImages() {
        return images;
    }

    /**
     * Sets the images.
     *
     * @param images The images to set
     */
    public void setImages(List<Image> images) {
        this.images = images;
    }

    /**
     * Gets the image width.
     *
     * @return The image width
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     * Sets the image width.
     *
     * @param imageWidth The image width to set
     */
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    /**
     * Gets the image height.
     *
     * @return The image height
     */
    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * Sets the image height.
     *
     * @param imageHeight The image height to set
     */
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    /**
     * Adds an image to the list of images.
     * Initializes the list if it's null.
     *
     * @param image The image to add
     */
    public void addImage(Image image) {
        if(images == null){
            images = new ArrayList<>();
        }
        images.add(image);
    }
}
