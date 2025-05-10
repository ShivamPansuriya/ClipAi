//package com.example.ClipAI.service.audio;
//
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.python.core.PyObject;
//import org.python.util.PythonInterpreter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.UUID;
//
//@Slf4j
//@Service
//public class TTSService {
//    private PythonInterpreter pythonInterpreter;
//    private PyObject ttsModel;
//
//    @Value("${tts.output.directory:/tmp/tts-output}")
//    private String outputDirectory;
//
//    @Value("${tts.model.name:tts_models/en/vctk/vits}")
//    private String modelName;
//
//    @Value("${tts.speaker.wav.path}")
//    private String speakerWavPath;
//
//    @PostConstruct
//    public void init() {
//        try {
//            createOutputDirectory();
//            initializePythonEnvironment();
//            initializeTTSModel();
//        } catch (Exception e) {
//            log.error("Failed to initialize TTS service", e);
//            throw new TTSInitializationException("Failed to initialize TTS service", e);
//        }
//    }
//
//    private void createOutputDirectory() throws TTSInitializationException {
//        try {
//            Path directory = Paths.get(outputDirectory);
//            if (!Files.exists(directory)) {
//                Files.createDirectories(directory);
//            }
//        } catch (Exception e) {
//            throw new TTSInitializationException("Failed to create output directory", e);
//        }
//    }
//
//    private void initializePythonEnvironment() {
//        pythonInterpreter = new PythonInterpreter();
//
//        // Install and import required packages
//        pythonInterpreter.exec("""
//            import sys
//            import os
//
//            try:
//                import TTS
//            except ImportError:
//                import pip
//                pip.main(['install', 'TTS'])
//                import TTS
//            """);
//    }
//
//    private void initializeTTSModel() {
//        // Initialize TTS model with specified configuration
//        pythonInterpreter.exec(String.format("""
//            from TTS.api import TTS
//            tts = TTS.load_model('%s', gpu=False)
//            """, modelName));
//
//        ttsModel = pythonInterpreter.get("tts");
//    }
//
//    public File generateSpeech(String text) throws TTSGenerationException {
//        return generateSpeech(text, null);
//    }
//
//    public File generateSpeech(String text, TTSConfiguration config) throws TTSGenerationException {
//        if (text == null || text.trim().isEmpty()) {
//            throw new IllegalArgumentException("Text cannot be empty");
//        }
//
//        try {
//            String outputPath = generateOutputPath();
//            config = config != null ? config : TTSConfiguration.getDefault();
//
//            pythonInterpreter.set("text", text);
//            pythonInterpreter.set("output_path", outputPath);
//            pythonInterpreter.set("speaker_wav", speakerWavPath);
//
//            // Generate speech with specified configuration
//            String pythonCommand = String.format("""
//                tts.tts_to_file(
//                    text=text,
//                    file_path=output_path,
//                    speaker_wav=speaker_wav,
//                    language='%s',
//                    speed=%f
//                )
//                """,
//                    config.getLanguage(),
//                    config.getSpeed()
//            );
//
//            pythonInterpreter.exec(pythonCommand);
//
//            File outputFile = new File(outputPath);
//            if (!outputFile.exists()) {
//                throw new TTSGenerationException("Failed to generate audio file");
//            }
//
//            return outputFile;
//        } catch (Exception e) {
//            throw new TTSGenerationException("Failed to generate speech", e);
//        }
//    }
//
//    private String generateOutputPath() {
//        return Paths.get(outputDirectory, UUID.randomUUID().toString() + ".wav").toString();
//    }
//}
//
//class TTSConfiguration {
//    private String language;
//    private double speed;
//
//    public TTSConfiguration(String language, double speed) {
//        this.language = language;
//        this.speed = speed;
//    }
//
//    public static TTSConfiguration getDefault() {
//        return new TTSConfiguration("en", 1.0);
//    }
//
//    public String getLanguage() {
//        return language;
//    }
//
//    public double getSpeed() {
//        return speed;
//    }
//}
//
//class TTSInitializationException extends RuntimeException {
//    public TTSInitializationException(String message, Throwable cause) {
//        super(message, cause);
//    }
//}
//
//class TTSGenerationException extends Exception {
//    public TTSGenerationException(String message) {
//        super(message);
//    }
//
//    public TTSGenerationException(String message, Throwable cause) {
//        super(message, cause);
//    }
//}
