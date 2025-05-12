package com.example.ClipAI.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for file operations.
 * Provides methods for file manipulation.
 */
@Component
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    
    /**
     * Ensures a directory exists, creating it if necessary.
     *
     * @param directoryPath The path of the directory to ensure
     * @throws IOException If an I/O error occurs
     */
    public void ensureDirectoryExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Created directory: {}", directoryPath);
        }
    }
    
    /**
     * Saves binary data to a file.
     *
     * @param data The binary data to save
     * @param filePath The path where the file should be saved
     * @throws IOException If an I/O error occurs
     */
    public void saveBinaryFile(byte[] data, String filePath) throws IOException {
        // Ensure parent directory exists
        Path path = Paths.get(filePath);
        ensureDirectoryExists(path.getParent().toString());
        
        // Write file
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
            fos.flush();
        }
        logger.info("Saved file: {}", filePath);
    }
    
    /**
     * Gets PNG files in a directory, sorted by creation time.
     *
     * @param directoryPath The directory to search
     * @return List of file names sorted by creation time
     */
    public List<String> getPNGFilesSortedByCreationTime(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            
            if (files == null || files.length == 0) {
                return List.of();
            }
            
            return Arrays.stream(files)
                    .sorted(Comparator.comparing(file -> {
                        try {
                            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                            return attr.creationTime();
                        } catch (IOException e) {
                            logger.error("Error reading file attributes: {}", file.getName(), e);
                            return null;
                        }
                    }))
                    .map(File::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error listing PNG files: {}", directoryPath, e);
            return List.of();
        }
    }
    
    /**
     * Deletes a file.
     *
     * @param directoryPath The directory containing the file
     * @param fileName The name of the file to delete
     * @return true if the file was deleted, false otherwise
     */
    public boolean deleteFile(String directoryPath, String fileName) {
        try {
            Path filePath = Paths.get(directoryPath, fileName);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                logger.info("Deleted file: {}", filePath);
            } else {
                logger.warn("File not found for deletion: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            logger.error("Error deleting file: {}/{}", directoryPath, fileName, e);
            return false;
        }
    }
}
