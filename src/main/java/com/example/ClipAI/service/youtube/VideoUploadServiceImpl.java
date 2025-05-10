package com.example.ClipAI.service.youtube;

import com.example.ClipAI.config.YouTubeConfig;
import com.example.ClipAI.model.youtube.VideoRequest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.libfreenect._freenect_device;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoUploadServiceImpl implements VideoUploadService{

    @Value("${youtube.application-name}")
    private String applicationName;

    private final YouTubeCredentialManager credentialManager;

    private  final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    public VideoUploadServiceImpl(YouTubeCredentialManager credentialManager) {
        this.credentialManager = credentialManager;
    }

    public VideoRequest uploadVideoWithDelay(
            long userId,
            VideoRequest videoRequest
    ) {

        videoRequest.setPrivacyStatus("unlisted");
        uploadVideo(userId, videoRequest);
//
//        LocalTime now = LocalTime.now();
//        LocalTime targetTime = now.plusHours(videoRequest.getUploadDelay()); // 7 PM
//
//        // Calculate the initial delay
        long initialDelay = videoRequest.getUploadDelay();
//        if (now.isBefore(targetTime))
//        {
//            initialDelay = Duration.between(now, targetTime).getSeconds();
//        }
//        else
//        {
//            // If it's already past 7 PM today, schedule for 7 PM tomorrow
//            initialDelay = Duration.between(now, targetTime.plusHours(24)).getSeconds() + 86400L;
//        }
//        initialDelay /= 60;
        System.out.println(initialDelay);
        executor.schedule(new UploadHelper(videoRequest.getVideoId(), userId, credentialManager), initialDelay, TimeUnit.MINUTES);

        return videoRequest;
    }

    public VideoRequest uploadVideo(long userId, VideoRequest videoRequest){
        Credential credential = null;
        try
        {
            credential = credentialManager.getStoredCredential(userId);

            if (credential == null)
            {
                credential = credentialManager.createCredential(userId);
                if (credential == null)
                {
                    throw new IllegalStateException("No credentials found for user: " + userId);
                }
            }

            YouTube youtube = new YouTube.Builder(YouTubeConfig.HTTP_TRANSPORT, YouTubeConfig.JSON_FACTORY, credential).setApplicationName(applicationName)
                    .build();

            Video video = new Video();

            // Set video metadata
            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(videoRequest.getTitle());
            snippet.setDescription(videoRequest.getDescription());
//        snippet.setTags(videoRequest.getTags());
            snippet.setDefaultAudioLanguage("en-US");
            snippet.setDefaultLanguage("en-US");
            video.setSnippet(snippet);

            // Set video privacy status
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus(videoRequest.getPrivacyStatus()); // "private", "public", or "unlisted"
            status.setMadeForKids(false);
            video.setStatus(status);

            // Create the video insert request

            while (videoRequest.getVideoId() == null || videoRequest.getVideoId().isEmpty())
            {
                YouTube.Videos.Insert videoInsert = youtube.videos()
                        .insert(List.of("snippet", "status"), video, new InputStreamContent("video/*", new FileInputStream(videoRequest.getVideoPath())));

                // Execute upload
                Video returnedVideo = videoInsert.execute();
                videoRequest.setVideoId(returnedVideo.getId());
            }
            log.info("uploaded video : {}, tags :{}", videoRequest.getTitle(), videoRequest.getTags());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return videoRequest;
    }
}
