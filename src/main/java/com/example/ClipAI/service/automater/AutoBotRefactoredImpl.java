package com.example.ClipAI.service.automater;

import com.example.ClipAI.config.AppConfig;
import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.youtube.VideoRequest;
import com.example.ClipAI.service.ImageGenerationService;
import com.example.ClipAI.service.VideoCreatorRefactored;
import com.example.ClipAI.service.audio.AudioService;
import com.example.ClipAI.service.chat.ChatType;
import com.example.ClipAI.service.chat.ShortsGeminiServiceRefactoredImpl;
import com.example.ClipAI.service.youtube.VideoUploadService;
import com.example.ClipAI.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the AutoBotService interface.
 * Provides functionality for automating video creation and upload.
 */
@Service
public class AutoBotRefactoredImpl implements AutoBotService {
    private final Logger logger = LoggerFactory.getLogger(AutoBotRefactoredImpl.class);
    
    @Autowired
    private ImageGenerationService imageGenerationService;
    
    @Autowired
    private ShortsGeminiServiceRefactoredImpl shortsGeminiService;
    
    @Autowired
    private VideoCreatorRefactored videoCreator;
    
    @Autowired
    private AudioService audioService;
    
    @Autowired
    private VideoUploadService videoUploadService;
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private FileUtils fileUtils;
    
    private final Map<String, List<String>> topic = new HashMap<>();
    private static final Map<String, Integer> indexMap = new HashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    private ScheduledFuture<?> scheduledFuture;
    
    @Override
    public boolean startAutoBot(String userId, ChatType chatType) {
        indexMap.put(userId, 0);
        scheduledFuture = executor.scheduleAtFixedRate(() -> {
            if (topic.containsKey(userId) && !topic.get(userId).isEmpty() && indexMap.get(userId) < topic.get(userId).size()) {
                try {
                    createAndUploadVideo(userId, chatType);
                } catch (Exception e) {
                    logger.error("Error in first attempt to create and upload video: {}", e.getMessage(), e);
                    try {
                        createAndUploadVideo(userId, chatType);
                    } catch (IOException ex) {
                        logger.error("Error in second attempt to create and upload video: {}", ex.getMessage(), ex);
                    }
                }
            }
        }, 0, 24, TimeUnit.HOURS);
        return true;
    }
    
    @Override
    public void createAndUploadVideo(String userId, ChatType chatType) throws IOException {
        logger.info("Creating and uploading video for user: {}, topic: {}", userId, topic.get(userId).get(indexMap.get(userId)));
        
        ClipAIRest clipAIRest = new ClipAIRest();
        clipAIRest.setTopic(topic.get(userId).get(indexMap.get(userId)));
        clipAIRest.setImageHeight(1280);
        clipAIRest.setImageWidth(720);
        
        // Generate script and images
        shortsGeminiService.generateScript(clipAIRest, chatType);
        shortsGeminiService.generateImagesPrompts(clipAIRest);
        shortsGeminiService.filterImages(clipAIRest);
        imageGenerationService.generateAndSaveImage(clipAIRest);
        
        // Generate audio
        generateAudio(clipAIRest);
        
        // Create video
        videoCreator.createVideo(
                appConfig.getAudioOutputPath(),
                appConfig.getOutputDirectory(),
                appConfig.getVideoOutputPath(),
                clipAIRest
        );
        
        // Prepare video request
        VideoRequest request = new VideoRequest();
        request.setVideoPath("output_video.mp4");
        shortsGeminiService.generateMetaData(clipAIRest, request);
        logger.info("Generated metadata: {}", request.getTitle());
        
        // Upload video
        videoUploadService.uploadVideoWithDelay(Long.parseLong(userId), request);
        
        // Clean up
        List<String> fileNames = fileUtils.getPNGFilesSortedByCreationTime(appConfig.getOutputDirectory());
        fileNames.forEach(fileName -> fileUtils.deleteFile(appConfig.getOutputDirectory(), fileName));
        
        // Update index
        indexMap.put(userId, indexMap.get(userId) + 1);
        logger.info("Video creation and upload completed for user: {}", userId);
    }
    
    @Override
    public void setTopic(String userId, List<String> topics) {
        if (this.topic.containsKey(userId)) {
            this.topic.get(userId).addAll(topics);
        } else {
            this.topic.put(userId, topics);
        }
        logger.info("Set topics for user: {}, count: {}", userId, topics.size());
    }
    
    /**
     * Generates audio from the script.
     *
     * @param clipAIRest The ClipAIRest object containing the script
     */
    private void generateAudio(ClipAIRest clipAIRest) {
        // Command to be executed
        String command = "python main.py \"%s\""
                .formatted(clipAIRest.getScript().trim());
        
        try {
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
            logger.info("Audio generation output: {}", output);
            
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            if (errorOutput.length() > 0) {
                logger.error("Audio generation error output: {}", errorOutput);
            }
            
            // Wait for the process to finish and get the exit code
            int exitCode = process.waitFor();
            logger.info("Audio generation command executed with exit code: {}", exitCode);
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error generating audio: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void printDelay(String userId) {
        if (scheduledFuture == null) {
            logger.warn("No scheduled task for user: {}", userId);
            return;
        }
        
        long initialDelay = scheduledFuture.getDelay(TimeUnit.MILLISECONDS);
        
        long hoursLeft = TimeUnit.MILLISECONDS.toHours(initialDelay);
        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(initialDelay) - TimeUnit.HOURS.toMinutes(hoursLeft);
        long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(initialDelay) - TimeUnit.MINUTES.toSeconds(minutesLeft) - TimeUnit.HOURS.toSeconds(hoursLeft);
        
        logger.info("Time remaining for next run: {}:{}:{}", hoursLeft, minutesLeft, secondsLeft);
        logger.info("Index for user {}: {}", userId, indexMap.get(userId));
    }
}
