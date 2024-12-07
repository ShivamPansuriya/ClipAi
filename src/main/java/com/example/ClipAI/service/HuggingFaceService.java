package com.example.ClipAI.service;

import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.Image;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

@Service
public class HuggingFaceService {
    private static final String HUGGING_FACE_URL =
            "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-3.5-large";
    private static final String API_KEY = "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";
    private final String outputPath = "/home/shivam/Documents/shorts/ClipAI/src/main/resources/static";
    private final Logger logger = LoggerFactory.getLogger(HuggingFaceService.class);
    private final OkHttpClient client =
            new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS).callTimeout(240, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

    public boolean generateAndSaveImage(Image image, int level) {
        logger.debug("requesting image:{}",image.getKey());
        boolean result = false;
        try {
            // Create output directory if it doesn't exist
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Prepare JSON payload
            String jsonPayload = String.format("{\"inputs\": \"%s\",\"parameters\": {\"guidance_scale\" : 5, \"width\": %d, \"height\": %d}}", image.getDescription(), image.getWidth(), image.getHeight());

            // Create request
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonPayload);
            Request request = new Request.Builder().url(HUGGING_FACE_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY).post(body).build();

            // Execute request
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (level < 5) {
                        logger.error("Unexpected response code: {} for image: {}", response.code(),
                                image.getKey());
                        return generateAndSaveImage(image, ++level);
                    }
                    return false;
                }

                // Save the image
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    // Create full file path with .png extension
                    String fileName = image.getKey().replaceAll("[^a-zA-Z0-9.-]", "_") + ".png";
                    File outputFile = new File(outputDir.toFile(), fileName);

                    // Save image data to file
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(responseBody.bytes());
                        logger.info("Successfully saved image: {}", fileName);
                        result = true;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while generating/saving image: {} - {}", image.getKey(), e.getMessage());
        }
        return result;
    }

    public void generateAndSaveImage(ClipAIRest clipAIRest) {
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