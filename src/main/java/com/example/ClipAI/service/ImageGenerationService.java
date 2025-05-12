package com.example.ClipAI.service;

import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.Image;

/**
 * Interface for image generation services.
 * Defines methods for generating and saving images.
 */
public interface ImageGenerationService {
    
    /**
     * Generates and saves a single image.
     *
     * @param image The image to generate
     * @param level Retry level (for recursive retries)
     * @return true if successful, false otherwise
     */
    boolean generateAndSaveImage(Image image, int level);
    
    /**
     * Generates and saves all images in the ClipAIRest object.
     *
     * @param clipAIRest The ClipAIRest object containing images to generate
     */
    void generateAndSaveImage(ClipAIRest clipAIRest);
}
