package com.example.ClipAI.service.automater;

import com.example.ClipAI.service.chat.ChatType;

import java.io.IOException;
import java.util.List;

/**
 * Interface for the AutoBot service.
 * Defines methods for automating video creation and upload.
 */
public interface AutoBotService {
    
    /**
     * Starts the AutoBot for the given user and chat type.
     *
     * @param userId The user ID
     * @param chatType The chat type to use
     * @return true if successful, false otherwise
     */
    boolean startAutoBot(String userId, ChatType chatType);
    
    /**
     * Sets the topics for the given user.
     *
     * @param userId The user ID
     * @param topics The topics to set
     */
    void setTopic(String userId, List<String> topics);
    
    /**
     * Creates and uploads a video for the given user.
     *
     * @param userId The user ID
     * @param chatType The chat type to use
     * @throws IOException If an error occurs
     */
    void createAndUploadVideo(String userId, ChatType chatType) throws IOException;
    
    /**
     * Prints the delay for the given user.
     *
     * @param userId The user ID
     */
    void printDelay(String userId);
}
