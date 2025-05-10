package com.example.ClipAI.service.youtube;

import com.example.ClipAI.config.YouTubeConfig;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoStatus;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class UploadHelper implements Runnable {

    @Value("${youtube.application-name}")
    private String applicationName;

    private final String videoId;

    private final long  userId;

    private final YouTubeCredentialManager credentialManager;

    public UploadHelper(String  videoId, long userId, YouTubeCredentialManager credential) {
        this.videoId = videoId;
        this.userId = userId;
        this.credentialManager = credential;
    }

    @Override
    public void run() {
        Credential credential = null;
        try {
            credential = credentialManager.getStoredCredential(userId);

            if (credential == null) {
                throw new IllegalStateException("No credentials found for user: " + userId);
            }

            YouTube youtube = new YouTube.Builder(
                    YouTubeConfig.HTTP_TRANSPORT,
                    YouTubeConfig.JSON_FACTORY,
                    credential)
                    .setApplicationName(applicationName)
                    .build();

            Video video = new Video();
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            video.setStatus(status);
            video.setId(videoId);

            // Update video status to public
            YouTube.Videos.Update request = youtube.videos()
                    .update(List.of("status"), video);
            Video updatedVideo = request.execute();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
