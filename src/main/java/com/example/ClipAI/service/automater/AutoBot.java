package com.example.ClipAI.service.automater;

import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.youtube.VideoRequest;
import com.example.ClipAI.service.HuggingFaceService;
import com.example.ClipAI.service.VideoCreator;
import com.example.ClipAI.service.audio.AudioService;
import com.example.ClipAI.service.chat.ChatType;
import com.example.ClipAI.service.chat.ShortsGeminiServiceImpl;
import com.example.ClipAI.service.youtube.VideoUploadService;
import com.example.ClipAI.service.youtube.YoutubeMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AutoBot {
    @Autowired
    private HuggingFaceService huggingFaceService;

    @Autowired
    private ShortsGeminiServiceImpl shortsGeminiService;

    @Autowired
    private VideoCreator videoCreator;

    @Autowired
    private AudioService audioService;

    @Autowired
    private VideoUploadService videoUploadService;

    private final Map<String, List<String> > topic = new HashMap<>();

    private static Map<String,Integer> indexMap = new HashMap<>();

    private final String outputPath = "src\\main\\resources\\static";

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    private ScheduledFuture<?> scheduledFuture;

    public boolean startAutoBot(String userId,ChatType chatType) {
        indexMap.put(userId,0);
        scheduledFuture = executor.scheduleAtFixedRate(()->{
            if(topic.containsKey(userId) && !topic.get(userId).isEmpty() && indexMap.get(userId)<topic.get(userId).size()) {
                try {
                    createAndUploadVideo(userId, chatType);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        createAndUploadVideo(userId, chatType);
                    } catch (IOException ex) {
                        e.printStackTrace();
                    }
                }
            }
        },0,24, TimeUnit.HOURS);
        return true;
    }

    private void createAndUploadVideo(String userId, ChatType chatType) throws IOException {
ClipAIRest clipAIRest = new ClipAIRest();
                clipAIRest.setTopic(topic.get(userId).get(indexMap.get(userId)));
        clipAIRest.setImageHeight(1280);
        clipAIRest.setImageWidth(720);

        shortsGeminiService.generateScript(clipAIRest, chatType);
        shortsGeminiService.generateImagesPrompts(clipAIRest);
        shortsGeminiService.filterImages(clipAIRest);
        huggingFaceService.generateAndSaveImage(clipAIRest);
        generateAudio(clipAIRest);
        videoCreator.createVideo("src/main/resources/static/narration.mp3", outputPath,
                "final_video.mp4", clipAIRest);
        VideoRequest request = new VideoRequest();
        request.setVideoPath("output_video.mp4");
        shortsGeminiService.generateMetaData(clipAIRest, request);
        System.out.println(clipAIRest);
        request.setTitle(request.getTitle());
        videoUploadService.uploadVideoWithDelay(Long.parseLong(userId), request);
        List<String> fileNames = getPNGFilesSortedByCreationTime("src/main/resources/static/");
        fileNames.forEach(fileName->deleteFile("src/main/resources/static/",fileName));
        indexMap.put(userId,indexMap.get(userId)+1);
    }

    public void setTopic(String userId, List<String> topic) {
        if(this.topic.containsKey(userId)) {
            this.topic.get(userId).addAll(topic);
        }
        else{
            this.topic.put(userId, topic);
        }
    }

    private void generateAudio(ClipAIRest clipAIRest){
        // Command to be executed
        String command = "python main.py \"%s\""
                .formatted(clipAIRest.getScript().trim()); // Replace with your desired command

        try {
            // Create a Process to execute the command
            Process process = Runtime.getRuntime().exec(command);

            // Capture the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            System.out.println("Output:");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            System.out.println("Errors (if any):");
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }

            // Wait for the process to finish and get the exit code
            int exitCode = process.waitFor();
            System.out.println("Command executed with exit code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void printDelay(String userId){
        long initialDelay = scheduledFuture.getDelay(TimeUnit.MILLISECONDS); // Gets the delay until the next run

        long hoursLeft = TimeUnit.MILLISECONDS.toHours(initialDelay);
        long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(initialDelay) - TimeUnit.HOURS.toMinutes(hoursLeft);
        long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(initialDelay) - TimeUnit.MINUTES.toSeconds(minutesLeft) - TimeUnit.HOURS.toSeconds(hoursLeft);

        System.out.printf("Time remaining for next run: %02d:%02d:%02d\n", hoursLeft, minutesLeft, secondsLeft);
        System.out.printf("index for user=%d",indexMap.get(userId));
    }

    public static List<String> getPNGFilesSortedByCreationTime(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

            if (files == null) {
                return new ArrayList<>();
            }
            List<String> sortedFileNames = new ArrayList<>();
            for (File file : files) {
                sortedFileNames.add(file.getName());
            }
            return sortedFileNames;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Deletes a file from the specified directory
     * @param directoryPath Directory containing the file
     * @param fileName Name of the file to delete
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteFile(String directoryPath, String fileName) {
        try {
            Path filePath = Paths.get(directoryPath, fileName);
            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
