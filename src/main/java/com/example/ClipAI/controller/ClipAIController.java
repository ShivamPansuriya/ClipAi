package com.example.ClipAI.controller;

import com.example.ClipAI.model.AutomationTopic;
import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.youtube.VideoRequest;
import com.example.ClipAI.model.youtube.YoutubeMonitor;
import com.example.ClipAI.service.HuggingFaceService;
import com.example.ClipAI.service.VideoCreator;
import com.example.ClipAI.service.audio.AudioService;
import com.example.ClipAI.service.automater.AutoBot;
import com.example.ClipAI.service.chat.AnimationGeminiServiceImpl;
import com.example.ClipAI.service.chat.ChatType;
import com.example.ClipAI.service.chat.ShortsGeminiServiceImpl;
import com.example.ClipAI.service.youtube.VideoUploadService;
import com.example.ClipAI.service.youtube.YoutubeMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class ClipAIController {
    private final String AUDIO_UPLOAD_DIR="src\\main\\resources\\static\\audioTranscribe";

    @Autowired
    private HuggingFaceService huggingFaceService;

    @Autowired
    private ShortsGeminiServiceImpl shortsGeminiService;

    @Autowired
    private AnimationGeminiServiceImpl animationGeminiService;

    @Autowired
    private VideoCreator videoCreator;

    @Autowired
    private AudioService audioService;

    @Autowired
    private VideoUploadService videoUploadService;

    @Autowired
    private AutoBot autoBot;

    @Autowired
    private YoutubeMonitorService youtubeMonitorService;

    private final String outputPath = "src/main/resources/static";

    @PostMapping(value = "v1/generate/video")
    public ClipAIRest generateVideo(@RequestBody ClipAIRest clipAIRest) {
        shortsGeminiService.generateScript(clipAIRest, ChatType.HISTORY_SCHOKING);
        shortsGeminiService.generateImagesPrompts(clipAIRest);
        shortsGeminiService.filterImages(clipAIRest);
        huggingFaceService.generateAndSaveImage(clipAIRest);
        return clipAIRest;
    }


    @PostMapping(value = "v1/generate/videos")
    public ClipAIRest generateVideos(@RequestBody ClipAIRest clipAIRest) {
        videoCreator.createVideo("src/main/resources/static/narration.mp3",outputPath, "final_video.mp4", clipAIRest);
        return clipAIRest;
    }

    @PostMapping(value = "v1/generate/script")
    public ClipAIRest generateScript(@RequestBody ClipAIRest clipAIRest, @RequestParam ChatType chatType) {
        shortsGeminiService.generateScript(clipAIRest, chatType);
        return clipAIRest;
    }

    @PostMapping(value = "v1/generate/image")
        public ClipAIRest generateImageDescription(@RequestBody ClipAIRest clipAIRest, @RequestParam String type) {
        if(type.equals("animation")){
            animationGeminiService.generateImagesPrompts(clipAIRest);
        }
        else {
            shortsGeminiService.generateImagesPrompts(clipAIRest);
        }
        return clipAIRest;
    }

    @PostMapping(value = "v1/generate/images")
    public ClipAIRest generateImages(@RequestBody ClipAIRest clipAIRest) {
        huggingFaceService.generateAndSaveImage(clipAIRest);
        return clipAIRest;
    }

    @PostMapping(value = "v1/transcribe/audio")
    public ClipAIRest transcribeAudio(@RequestBody MultipartFile audioFile) throws Exception {
        ClipAIRest clipAIRest = new ClipAIRest();

        try {
            // Ensure directory exists
            File uploadDir = new File(AUDIO_UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            // Save the file
            Path filePath = Paths.get(AUDIO_UPLOAD_DIR, audioFile.getOriginalFilename());
            Files.write(filePath, audioFile.getBytes());
            clipAIRest.setScript(audioService.transcribeAudio(AUDIO_UPLOAD_DIR, null).getText());
            return clipAIRest;
        } catch (IOException e) {
            return clipAIRest;
        }
    }

    @PostMapping("v1/youtube/{userId}/upload")
    public VideoRequest uploadVideo(@RequestBody VideoRequest request,@PathVariable("userId") long userId) throws Exception {
        try {
             return videoUploadService.uploadVideoWithDelay(userId, request);
        } catch (Exception e) {
            throw new Exception(e);
//            request.setVideoId(null);
//            return request;
        }
    }

    @PostMapping("v1/youtube/{userId}/{chatType}/automate")
    public boolean startAutomate(@PathVariable("userId") long userId, @PathVariable("chatType") ChatType chatType)
    {
        return autoBot.startAutoBot(String.valueOf(userId), chatType);
    }

    @PostMapping("v1/youtube/{userId}/topic")
    public boolean addTopics(@PathVariable("userId") String  userId, @RequestBody AutomationTopic automationTopic) {
        autoBot.setTopic(userId,automationTopic.getTopic());
        return true;
    }

    @PostMapping("v1/youtube/description")
    public VideoRequest startAutomatess(@RequestBody ClipAIRest clipAIRest) {
        return shortsGeminiService.generateMetaData(clipAIRest, new VideoRequest());
    }

    @PostMapping("/generate")
    public boolean generateSpeech(@RequestBody ClipAIRest clipAIRest) {
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
        return true;
    }

    @GetMapping("/{user_id}/delay")
    public void printDelay(@PathVariable("user_id") String  userId){
        autoBot.printDelay(userId);
    }

    @PostMapping("/{user_id}/autobot")
    public void printDelay(@PathVariable("user_id") String  userId, @RequestBody YoutubeMonitor youtubeMonitor){
        youtubeMonitor.setUserId(Long.parseLong(userId));
        youtubeMonitorService.checkAndUploadVideo(youtubeMonitor);
    }

    @PostMapping("/{user_id}/autobot/addchannel")
    public void addChannel(@PathVariable("user_id") String  userId, @RequestBody YoutubeMonitor youtubeMonitor){
        youtubeMonitor.setUserId(Long.parseLong(userId));
        youtubeMonitorService.addChannelIds(youtubeMonitor);
    }
}

//            client.generateAndSaveSpeech("Hello, this is a test!", "C:/output/speech.mp3");