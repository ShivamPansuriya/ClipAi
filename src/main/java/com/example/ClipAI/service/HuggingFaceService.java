package com.example.ClipAI.service;

import com.example.ClipAI.config.AppConfig;
import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.Image;
import com.example.ClipAI.util.FileUtils;
import com.example.ClipAI.util.HttpUtils;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Service for interacting with Hugging Face API to generate images.
 * Implements the ImageGenerationService interface.
 */
@Service
public class HuggingFaceService implements ImageGenerationService {
    private final Logger logger = LoggerFactory.getLogger(HuggingFaceService.class);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private HttpUtils httpUtils;

    @Autowired
    private FileUtils fileUtils;

    /**
     * Generates and saves a single image.
     *
     * @param image The image to generate
     * @param level Retry level (for recursive retries)
     * @return true if successful, false otherwise
     */
    public boolean generateAndSaveImage(Image image, int level) {
        logger.info("Requesting image: {}", image.getKey());
        boolean result = false;
        try {
            // Create output directory if it doesn't exist
            fileUtils.ensureDirectoryExists(appConfig.getOutputDirectory());

            // Prepare JSON payload
            String jsonPayload = String.format(
                "{\"inputs\": \"%s\",\"parameters\": {\"guidance_scale\" : 5, \"width\": %d, \"height\": %d}}",
                image.getDescription(),
                image.getWidth(),
                image.getHeight()
            );

            // Create headers
            Headers headers = httpUtils.createHeaders(
                "Authorization", "Bearer " + appConfig.getHuggingFaceApiKey(),
                "Content-Type", "application/json"
            );

            // Execute request
            try (Response response = httpUtils.postJson(appConfig.getHuggingFaceUrl(), jsonPayload, headers)) {
                // Save the image
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    // Create full file path with .png extension
                    String fileName = image.getKey().replaceAll("[^a-zA-Z0-9.-]", "_") + ".png";
                    Path outputPath = Paths.get(appConfig.getOutputDirectory(), fileName);

                    // Save image data to file
                    fileUtils.saveBinaryFile(responseBody.bytes(), outputPath.toString());
                    logger.info("Successfully saved image: {}", fileName);
                    result = true;
                }
            }
        } catch (IOException e) {
            logger.error("Error while generating/saving image: {} - {}", image.getKey(), e.getMessage());
            // Retry logic
            if (level < 5) {
                logger.info("Retrying image generation for: {}, attempt: {}", image.getKey(), level + 1);
                try {
                    Thread.sleep(2000); // Wait before retry
                    return generateAndSaveImage(image, level + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return result;
    }

    /**
     * Generates and saves all images in the ClipAIRest object.
     *
     * @param clipAIRest The ClipAIRest object containing images to generate
     */
    public void generateAndSaveImage(ClipAIRest clipAIRest) {
        if (clipAIRest.getImages() == null || clipAIRest.getImages().isEmpty()) {
            logger.warn("No images to generate");
            return;
        }

        Queue<Image> images = new ArrayDeque<>(clipAIRest.getImages());
        int maxRetries = 5;
        int currentRetry = 0;

        while (!images.isEmpty() && currentRetry < maxRetries) {
            Queue<Image> failedImages = new ArrayDeque<>();

            while (!images.isEmpty()) {
                Image image = images.poll();
                boolean result = generateAndSaveImage(image, 0);
                if (!result) {
                    failedImages.add(image);
                    logger.warn("Failed to generate image: {}. Will retry.", image.getKey());
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (!failedImages.isEmpty()) {
                images = failedImages;
                currentRetry++;
                if (currentRetry < maxRetries) {
                    try {
                        logger.info("Waiting before retrying failed images. Attempt {}/{}", currentRetry + 1,
                                maxRetries);
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}