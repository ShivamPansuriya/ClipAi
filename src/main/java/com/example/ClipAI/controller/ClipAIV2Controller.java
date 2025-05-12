package com.example.ClipAI.controller;

import com.example.ClipAI.config.AppConfig;
import com.example.ClipAI.model.AutomationTopic;
import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.youtube.VideoRequest;
import com.example.ClipAI.service.ImageGenerationService;
import com.example.ClipAI.service.VideoCreatorRefactored;
import com.example.ClipAI.service.audio.AudioService;
import com.example.ClipAI.service.automater.AutoBotService;
import com.example.ClipAI.service.chat.AnimationGeminiServiceRefactoredImpl;
import com.example.ClipAI.service.chat.ChatType;
import com.example.ClipAI.service.chat.ShortsGeminiServiceRefactoredImpl;
import com.example.ClipAI.service.youtube.VideoUploadService;
import com.example.ClipAI.service.youtube.YoutubeMonitorService;
import com.example.ClipAI.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for ClipAI application v2 API.
 * Provides REST endpoints for video generation and management.
 */
@RestController
@RequestMapping("/api/v2")
public class ClipAIV2Controller {
    private final Logger logger = LoggerFactory.getLogger(ClipAIV2Controller.class);
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private ImageGenerationService imageGenerationService;
    
    @Autowired
    private ShortsGeminiServiceRefactoredImpl shortsGeminiService;
    
    @Autowired
    private AnimationGeminiServiceRefactoredImpl animationGeminiService;
    
    @Autowired
    private VideoCreatorRefactored videoCreator;
    
    @Autowired
    private AudioService audioService;
    
    @Autowired
    private VideoUploadService videoUploadService;
    
    @Autowired
    private AutoBotService autoBotService;
    
    @Autowired
    private YoutubeMonitorService youtubeMonitorService;
    
    @Autowired
    private FileUtils fileUtils;
    
    /**
     * Generates a video by creating a script, generating image prompts, and generating images.
     *
     * @param clipAIRest The ClipAIRest object containing the topic
     * @return The updated ClipAIRest object
     */
    @PostMapping(value = "/generate/video")
    public ResponseEntity<ClipAIRest> generateVideo(@RequestBody ClipAIRest clipAIRest) {
        try {
            shortsGeminiService.generateScript(clipAIRest, ChatType.HISTORY_SCHOKING);
            shortsGeminiService.generateImagesPrompts(clipAIRest);
            shortsGeminiService.filterImages(clipAIRest);
            imageGenerationService.generateAndSaveImage(clipAIRest);
            return ResponseEntity.ok(clipAIRest);
        } catch (Exception e) {
            logger.error("Error generating video: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Creates a video from images and audio.
     *
     * @param clipAIRest The ClipAIRest object containing the images
     * @return The updated ClipAIRest object
     */
    @PostMapping(value = "/generate/videos")
    public ResponseEntity<ClipAIRest> generateVideos(@RequestBody ClipAIRest clipAIRest) {
        try {
            videoCreator.createVideo(
                appConfig.getAudioOutputPath(),
                appConfig.getOutputDirectory(),
                appConfig.getVideoOutputPath(),
                clipAIRest
            );
            return ResponseEntity.ok(clipAIRest);
        } catch (Exception e) {
            logger.error("Error creating video: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generates a script for the given topic and chat type.
     *
     * @param clipAIRest The ClipAIRest object containing the topic
     * @param chatType The type of chat to use for script generation
     * @return The updated ClipAIRest object
     */
    @PostMapping(value = "/generate/script")
    public ResponseEntity<ClipAIRest> generateScript(
            @RequestBody ClipAIRest clipAIRest,
            @RequestParam ChatType chatType) {
        try {
            shortsGeminiService.generateScript(clipAIRest, chatType);
            return ResponseEntity.ok(clipAIRest);
        } catch (Exception e) {
            logger.error("Error generating script: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generates image descriptions based on the script.
     *
     * @param clipAIRest The ClipAIRest object containing the script
     * @param type The type of images to generate (animation or regular)
     * @return The updated ClipAIRest object
     */
    @PostMapping(value = "/generate/image")
    public ResponseEntity<ClipAIRest> generateImageDescription(
            @RequestBody ClipAIRest clipAIRest,
            @RequestParam String type) {
        try {
            if (type.equals("animation")) {
                animationGeminiService.generateImagesPrompts(clipAIRest);
            } else {
                shortsGeminiService.generateImagesPrompts(clipAIRest);
            }
            return ResponseEntity.ok(clipAIRest);
        } catch (Exception e) {
            logger.error("Error generating image descriptions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generates images based on the image descriptions.
     *
     * @param clipAIRest The ClipAIRest object containing the image descriptions
     * @return The updated ClipAIRest object
     */
    @PostMapping(value = "/generate/images")
    public ResponseEntity<ClipAIRest> generateImages(@RequestBody ClipAIRest clipAIRest) {
        try {
            imageGenerationService.generateAndSaveImage(clipAIRest);
            return ResponseEntity.ok(clipAIRest);
        } catch (Exception e) {
            logger.error("Error generating images: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Transcribes audio to text.
     *
     * @param audioFile The audio file to transcribe
     * @return A ClipAIRest object containing the transcribed text
     */
    @PostMapping(value = "/transcribe/audio")
    public ResponseEntity<ClipAIRest> transcribeAudio(@RequestBody MultipartFile audioFile) {
        ClipAIRest clipAIRest = new ClipAIRest();
        
        try {
            // Ensure directory exists
            fileUtils.ensureDirectoryExists(appConfig.getAudioTranscribeDir());
            
            // Save the file
            Path filePath = Paths.get(appConfig.getAudioTranscribeDir(), audioFile.getOriginalFilename());
            fileUtils.saveBinaryFile(audioFile.getBytes(), filePath.toString());
            
            // Transcribe the audio
            clipAIRest.setScript(audioService.transcribeAudio(appConfig.getAudioTranscribeDir(), null).getText());
            return ResponseEntity.ok(clipAIRest);
        } catch (Exception e) {
            logger.error("Error transcribing audio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Uploads a video to YouTube.
     *
     * @param request The video request containing the video details
     * @param userId The user ID
     * @return The updated video request
     */
    @PostMapping("/youtube/{userId}/upload")
    public ResponseEntity<VideoRequest> uploadVideo(
            @RequestBody VideoRequest request,
            @PathVariable("userId") long userId) {
        try {
            return ResponseEntity.ok(videoUploadService.uploadVideoWithDelay(userId, request));
        } catch (Exception e) {
            logger.error("Error uploading video: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Starts the automation process for the given user and chat type.
     *
     * @param userId The user ID
     * @param chatType The chat type to use for automation
     * @return true if successful, false otherwise
     */
    @PostMapping("/youtube/{userId}/{chatType}/automate")
    public ResponseEntity<Boolean> startAutomate(
            @PathVariable("userId") long userId,
            @PathVariable("chatType") ChatType chatType) {
        try {
            boolean result = autoBotService.startAutoBot(String.valueOf(userId), chatType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error starting automation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Adds topics for the given user.
     *
     * @param userId The user ID
     * @param automationTopic The topics to add
     * @return true if successful, false otherwise
     */
    @PostMapping("/youtube/{userId}/topic")
    public ResponseEntity<Boolean> addTopics(
            @PathVariable("userId") String userId,
            @RequestBody AutomationTopic automationTopic) {
        try {
            autoBotService.setTopic(userId, automationTopic.getTopic());
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            logger.error("Error adding topics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generates metadata for a video.
     *
     * @param clipAIRest The ClipAIRest object containing the script
     * @return The video request with metadata
     */
    @PostMapping("/youtube/description")
    public ResponseEntity<VideoRequest> generateMetadata(@RequestBody ClipAIRest clipAIRest) {
        try {
            return ResponseEntity.ok(shortsGeminiService.generateMetaData(clipAIRest, new VideoRequest()));
        } catch (Exception e) {
            logger.error("Error generating metadata: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generates speech from text.
     *
     * @param clipAIRest The ClipAIRest object containing the script
     * @return true if successful, false otherwise
     */
    @PostMapping("/generate/speech")
    public ResponseEntity<Boolean> generateSpeech(@RequestBody ClipAIRest clipAIRest) {
        try {
            // Command to be executed
            String command = "python main.py \"%s\""
                    .formatted(clipAIRest.getScript().trim());
            
            // Create a Process to execute the command
            Process process = Runtime.getRuntime().exec(command);
            
            // Capture the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            logger.info("Command output: {}", output);
            
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            if (errorOutput.length() > 0) {
                logger.error("Command error output: {}", errorOutput);
            }
            
            // Wait for the process to finish and get the exit code
            int exitCode = process.waitFor();
            logger.info("Command executed with exit code: {}", exitCode);
            
            return ResponseEntity.ok(exitCode == 0);
        } catch (IOException | InterruptedException e) {
            logger.error("Error generating speech: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Prints the delay for the given user.
     *
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    @GetMapping("/youtube/{userId}/delay")
    public ResponseEntity<Boolean> printDelay(@PathVariable("userId") String userId) {
        try {
            autoBotService.printDelay(userId);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            logger.error("Error printing delay: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
