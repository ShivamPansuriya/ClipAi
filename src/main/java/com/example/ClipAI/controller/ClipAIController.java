package com.example.ClipAI.controller;

import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.service.HuggingFaceService;
import com.example.ClipAI.service.audio.AudioService;
import com.example.ClipAI.service.chat.AnimationGeminiService;
import com.example.ClipAI.service.chat.AnimationGeminiServiceImpl;
import com.example.ClipAI.service.chat.ShortsGeminiServiceImpl;
import com.example.ClipAI.service.VideoCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class ClipAIController {
    private final String AUDIO_UPLOAD_DIR="/home/shivam/Documents/shorts/ClipAI/src/main/resources/static/audioTranscribe";

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

    private final String outputPath = "/home/shivam/Documents/shorts/ClipAI/src/main/resources/static";

    @PostMapping(value = "v1/generate/video")
    public ClipAIRest generateVideo(@RequestBody ClipAIRest clipAIRest) {
        shortsGeminiService.generateScript(clipAIRest);
        shortsGeminiService.generateImagesPrompts(clipAIRest);
        huggingFaceService.generateAndSaveImage(clipAIRest);
        return clipAIRest;
    }


    @PostMapping(value = "v1/generate/videos")
    public ClipAIRest generateVideos(@RequestBody ClipAIRest clipAIRest) {
        videoCreator.createVideo("src/main/resources/static/narration.mp3",outputPath, "final_video.mp4", clipAIRest);
        return clipAIRest;
    }

    @PostMapping(value = "v1/generate/script")
    public ClipAIRest generateScript(@RequestBody ClipAIRest clipAIRest) {
        shortsGeminiService.generateScript(clipAIRest);
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
}
