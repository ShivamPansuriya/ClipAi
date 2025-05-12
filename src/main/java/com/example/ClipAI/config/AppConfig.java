package com.example.ClipAI.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Centralized configuration for the application.
 * This class provides access to all configuration properties.
 */
@Configuration
@Component
public class AppConfig {

    @Value("${app.output.directory:src/main/resources/static}")
    private String outputDirectory;

    @Value("${app.audio.output:src/main/resources/static/narration.mp3}")
    private String audioOutputPath;

    @Value("${app.video.output:src/main/resources/static/final_video.mp4}")
    private String videoOutputPath;

    @Value("${app.audio.transcribe.dir:src/main/resources/static/audioTranscribe}")
    private String audioTranscribeDir;

    // Hugging Face configuration
    @Value("${huggingface.api.url:https://api.inference.huggingface.co/models/stabilityai/stable-diffusion-3.5-large}")
    private String huggingFaceUrl;

    @Value("${huggingface.api.key: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}")
    private String huggingFaceApiKey;

    // Gemini configuration
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent}")
    private String geminiUrl;

    @Value("${gemini.api.key:AIzaSyA2OkjubYIKKz_URfe0PokvKPwp0dZ8VtQ}")
    private String geminiApiKey;

    // PlayHT configuration
    @Value("${playht.api.url:https://api.play.ht/api/v2/tts/stream}")
    private String playHtUrl;

    @Value("${playht.api.key:0c6942d1458b4842a30e12bb5953c292}")
    private String playHtApiKey;

    @Value("${playht.user.key:I08P5DnErXRmQSnlurRrlb3YirE3}")
    private String playHtUserKey;

    // YouTube configuration
    @Value("${youtube.client-id}")
    private String youtubeClientId;

    @Value("${youtube.client-secret}")
    private String youtubeClientSecret;

    @Value("${youtube.application-name}")
    private String youtubeApplicationName;

    @Value("${youtube.tokens-directory-path}")
    private String youtubeTokensDirectoryPath;

    // Getters
    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String getAudioOutputPath() {
        return audioOutputPath;
    }

    public String getVideoOutputPath() {
        return videoOutputPath;
    }

    public String getAudioTranscribeDir() {
        return audioTranscribeDir;
    }

    public String getHuggingFaceUrl() {
        return huggingFaceUrl;
    }

    public String getHuggingFaceApiKey() {
        return huggingFaceApiKey;
    }

    public String getGeminiUrl() {
        return geminiUrl;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getPlayHtUrl() {
        return playHtUrl;
    }

    public String getPlayHtApiKey() {
        return playHtApiKey;
    }

    public String getPlayHtUserKey() {
        return playHtUserKey;
    }

    public String getYoutubeClientId() {
        return youtubeClientId;
    }

    public String getYoutubeClientSecret() {
        return youtubeClientSecret;
    }

    public String getYoutubeApplicationName() {
        return youtubeApplicationName;
    }

    public String getYoutubeTokensDirectoryPath() {
        return youtubeTokensDirectoryPath;
    }
}
