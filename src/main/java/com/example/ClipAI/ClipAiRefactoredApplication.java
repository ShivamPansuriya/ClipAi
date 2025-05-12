package com.example.ClipAI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for ClipAI.
 * Uses the refactored architecture with API versioning.
 *
 * API Versions:
 * - v1: Original API endpoints (maintained for backward compatibility)
 * - v2: Refactored API endpoints with improved architecture
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.example.ClipAI.config",
        "com.example.ClipAI.controller",
        "com.example.ClipAI.model",
        "com.example.ClipAI.service",
        "com.example.ClipAI.util"
})
public class ClipAiRefactoredApplication {

    /**
     * Main method to start the application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ClipAiRefactoredApplication.class, args);
    }
}
