package com.example.ClipAI.model.audio;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TranscriptionResult {
    private String text;
    @JsonProperty(value = "segments")
    private List<TimedWord> segments;
    private String language;

    public TranscriptionResult(List<TimedWord> words, String text) {
        this.segments = words;
        this.text = text;
    }

    // Getters and setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<TimedWord> getSegments() { return segments; }
    public void setSegments(List<TimedWord> segments) { this.segments = segments; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
