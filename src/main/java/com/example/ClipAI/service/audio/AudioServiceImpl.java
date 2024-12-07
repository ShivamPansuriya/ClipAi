package com.example.ClipAI.service.audio;

import com.example.ClipAI.model.audio.TimedWord;
import com.example.ClipAI.model.audio.TranscriptionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for audio transcription using Whisper via command-line
 */
@Service
public class AudioServiceImpl implements AudioService {
    private static final Logger LOGGER = Logger.getLogger(AudioServiceImpl.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();


    // Transcription models
    public enum WhisperModel {
        TINY("tiny"),
        BASE("base"),
        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large");

        private final String modelName;

        WhisperModel(String modelName) {
            this.modelName = modelName;
        }

        public String getModelName() {
            return modelName;
        }
    }

    /**
     * Transcribe audio file
     *
     * @param audioPath Path to the audio file
     * @return TranscriptionResult containing transcription details
     * @throws Exception If transcription fails
     */
    public TranscriptionResult transcribeAudio(String audioPath) throws Exception {
        return transcribeAudio(audioPath, WhisperModel.BASE, null);
    }

    /**
     * Transcribe audio with specific model and language
     *
     * @param audioPath Path to the audio file
     * @param model     Whisper model to use
     * @param language  Optional language code (e.g., "en")
     * @return TranscriptionResult containing transcription details
     * @throws Exception If transcription fails
     */
    public TranscriptionResult transcribeAudio(String audioPath, WhisperModel model, String language)
            throws Exception {
        // Validate input
        validateAudioFile(audioPath);

        // Prepare output JSON path
        Path outputPath = Paths.get(System.getProperty("java.io.tmpdir"), "whisper_transcription.json");

        // Construct Whisper command
        List<String> command = buildWhisperCommand(audioPath, model, language, outputPath);

        // Execute transcription
        ProcessResult processResult = executeCommand(command);

        // Check for successful execution
        if (!processResult.isSuccess()) {
            throw new RuntimeException("Transcription failed: " + processResult.getErrorOutput());
        }

        // Read and parse JSON result
        return parseWordLevelTranscription(outputPath);
    }

    /**
     * Validate input audio file
     *
     * @param audioPath Path to the audio file
     * @throws IllegalArgumentException If file is invalid
     */
    private void validateAudioFile(String audioPath) {
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            throw new IllegalArgumentException("Audio file does not exist: " + audioPath);
        }

        // Add more validation (file type, size, etc.)
        String[] validExtensions = { ".wav", ".mp3", ".m4a", ".flac", ".ogg" };
        boolean validExtension = false;
        for (String ext : validExtensions) {
            if (audioPath.toLowerCase().endsWith(ext)) {
                validExtension = true;
                break;
            }
        }

        if (!validExtension) {
            throw new IllegalArgumentException(
                    "Unsupported audio file format. Supported: WAV, MP3, M4A, FLAC, OGG");
        }
    }

    /**
     * Build Whisper transcription command
     *
     * @param audioPath  Path to audio file
     * @param model      Whisper model
     * @param language   Optional language
     * @param outputPath Path for JSON output
     * @return List of command arguments
     */
    private List<String> buildWhisperCommand(String audioPath, WhisperModel model, String language,
            Path outputPath) {
        List<String> command = new ArrayList<>();
        command.add("whisper");
        command.add(audioPath);

        // Model selection
        command.add("--model");
        command.add(model.getModelName());

        // Optional language
        if (language != null && !language.isEmpty()) {
            command.add("--language");
            command.add(language);
        }

        // Output format
        command.add("--output_format");
        command.add("json");

        // Specify output directory
        command.add("--output_dir");
        command.add(outputPath.getParent().toString());

        //        command.add("--align_model");

        return command;
    }

    /**
     * Execute Whisper command
     *
     * @param command Whisper command to execute
     * @return ProcessResult with execution details
     * @throws Exception If command execution fails
     */
    private ProcessResult executeCommand(List<String> command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Capture output
        StringBuilder outputBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();

        try (BufferedReader outputReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = outputReader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null) {
                errorBuilder.append(line).append("\n");
            }
        }

        // Wait for process to complete
        int exitCode = process.waitFor();

        return new ProcessResult(exitCode, outputBuilder.toString(), errorBuilder.toString());
    }

    /**
     * Parse transcription result from JSON file
     *
     * @param outputPath Path to JSON output file
     * @return Parsed TranscriptionResult
     * @throws Exception If parsing fails
     */
    private TranscriptionResult parseTranscriptionResult(Path outputPath) throws Exception {
        // Find the JSON file (Whisper generates filename based on audio file)
        File jsonFile = findJsonResultFile(outputPath.getParent().toFile());

        if (jsonFile == null) {
            throw new RuntimeException("Transcription JSON result not found");
        }

        // Read and parse JSON
        String jsonContent = Files.readString(jsonFile.toPath());
        return MAPPER.readValue(jsonContent, TranscriptionResult.class);
    }

    private TranscriptionResult parseWordLevelTranscription(Path outputPath) throws Exception {
        File jsonFile = findJsonResultFile(outputPath.getParent().toFile());
        StringBuilder textBuilder = new StringBuilder();
        if (jsonFile == null) {
            throw new RuntimeException("Transcription JSON result not found");
        }

        String jsonContent = Files.readString(jsonFile.toPath());
        JsonNode rootNode = MAPPER.readTree(jsonContent);

        // Parse result to get each word's start and end time
        List<TimedWord> timedWords = new ArrayList<>();
        for (JsonNode segment : rootNode.path("segments")) {
            String text = segment.path("text").asText();
            double start = segment.path("start").asDouble();
            double end = segment.path("end").asDouble();
            double confidence = segment.path("confidence").asDouble(1.0);  // Optional confidence

            // Split sentence into words
            String[] words = text.replaceAll("[^a-zA-Z\\s]", "").toLowerCase().split("\\s+");
            List<String> wordsList = new ArrayList<>(Arrays.asList(words));
            wordsList.remove(0);
            double wordTime = (end - start)/wordsList.size();
            for (String word : wordsList) {
                end = start + wordTime - 0.02;
                textBuilder.append(word).append(" ");
                timedWords.add(new TimedWord(word, start, end, confidence));
                start = end+0.02;
            }
        }

        return new TranscriptionResult(timedWords, textBuilder.toString());
    }

    /**
     * Find the generated JSON result file
     *
     * @param directory Directory to search
     * @return JSON result file or null if not found
     */
    private File findJsonResultFile(File directory) {
        File[] jsonFiles = directory.listFiles((dir, name) -> name.endsWith(".json"));
        return jsonFiles != null && jsonFiles.length > 0 ? jsonFiles[0] : null;
    }

    /**
     * Process result from command execution
     */
    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String errorOutput;

        public ProcessResult(int exitCode, String output, String errorOutput) {
            this.exitCode = exitCode;
            this.output = output;
            this.errorOutput = errorOutput;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        public String getOutput() {
            return output;
        }

        public String getErrorOutput() {
            return errorOutput;
        }
    }

    /**
     * Transcription result model
     */

    /**
     * Example usage
     */
    //    public static void main(String[] args) {
    //        WhisperTranscriptionService service = new WhisperTranscriptionService();
    //
    //        try {
    //            // Basic transcription
    //            TranscriptionResult result = service.transcribeAudio("/path/to/your/audiofile.mp3");
    //
    //            // Print full transcription
    //            System.out.println("Full Text: " + result.getText());
    //            System.out.println("Detected Language: " + result.getLanguage());
    //
    //            // Print segments
    //            result.getSegments().forEach(segment ->
    //                    System.out.printf("Segment: %s (%.2f - %.2f, Confidence: %.2f)\n",
    //                            segment.getWord(),
    //                            segment.getStartTime(),
    //                            segment.getEndTime(),
    //                            segment.getConfidence())
    //            );
    //
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //    }
}