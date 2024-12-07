package com.example.ClipAI.service;

import com.example.ClipAI.model.ClipAIRest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class AudioGenerator {
    private final OkHttpClient client =
            new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

    private final String outputPath = "/home/shivam/Documents/shorts/ClipAI/src/main/resources/static";
    private final Logger logger = LoggerFactory.getLogger(AudioGenerator.class);

    public void generateAudio(ClipAIRest clipAIRest) {
        try {
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{\"voice\":\"s3://voice-cloning-zero-shot/d9ff78ba-d016-47f6-b0ef-dd630f59414e/female-cs/manifest.json\",\"output_format\":\"wav\",\"text\":\"hello there\",\"quality\":\"high\",\"sample_rate\":48000,\"temperature\":0.7,\"emotion\":\"male_happy\",\"voice_guidance\":3,\"style_guidance\":4,\"text_guidance\":2,\"language\":\"english\",\"seed\":2222}");
            Request request = new Request.Builder()
                    .url("https://api.play.ht/api/v2/tts/stream")
                    .post(body)
                    .addHeader("accept", "audio/mpeg")
                    .addHeader("content-type", "application/json")
                    .addHeader("AUTHORIZATION", "07ffd40363b847409dba4f3c8c51331e")
                    .addHeader("X-USER-ID", "8igMbkDCJ2OX2OwuLpNB7kdLIwf1")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                   logger.error("Unexpected response code: {} for image: ", response.code());
                   throw new RuntimeException("Unexpected response code: " + response.code());
                    }

                // Save the image
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    // Create full file path with .png extension
                    String fileName = clipAIRest.getTopic().replaceAll("[^a-zA-Z0-9.-]", "_") + ".wav";
                    File outputFile = new File(outputDir.toFile(), fileName);

                    // Save image data to file
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(responseBody.bytes());
                        logger.info("Successfully saved image: {}", fileName);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while saving audio: {} - ", clipAIRest.getTopic(), e);
        } catch (Exception e) {
            logger.error("Error while generating audio: {} - ", e.getMessage(), e);
        }
    }
}
