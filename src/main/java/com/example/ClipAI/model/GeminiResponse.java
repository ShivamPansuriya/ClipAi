package com.example.ClipAI.model;

import com.example.ClipAI.model.gemini.GeminiCandidate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    @JsonProperty("candidates")
    private List<GeminiCandidate> candidates;

    @JsonProperty("modelVersion")
    private String modelVersion;

    // Getters and Setters
    public List<GeminiCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GeminiCandidate> candidates) {
        this.candidates = candidates;
    }
}
