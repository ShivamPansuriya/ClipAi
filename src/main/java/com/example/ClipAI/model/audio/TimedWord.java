package com.example.ClipAI.model.audio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimedWord {
    @JsonProperty
    private double start;
    @JsonProperty
    private double end;
    @JsonProperty(value = "text")
    private String word;
    private double confidence;

    public TimedWord(String word, double start, double end, double confidence) {
        this.word = word;
        this.start = start;
        this.end = end;
        this.confidence = confidence;
    }
    // Getters and setters
    public double getStartTime() { return start; }
    public void setStartTime(double start) { this.start = start; }
    public double getEndTime() { return end; }
    public void setEndTime(double end) { this.end = end; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}