package com.example.ClipAI.model.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiContent {
    @JsonProperty("parts")
    private List<GeminiParts> parts;

    // Getters and Setters
    public List<GeminiParts> getParts() {
        return parts;
    }

    public void setParts(List<GeminiParts> parts) {
        this.parts = parts;
    }
}
