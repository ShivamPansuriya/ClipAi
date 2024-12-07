package com.example.ClipAI.service.audio;

import com.example.ClipAI.model.audio.TranscriptionResult;

public interface AudioService {
    public TranscriptionResult transcribeAudio(String audioPath) throws Exception;
    public TranscriptionResult transcribeAudio(String audioPath, AudioServiceImpl.WhisperModel model, String language) throws Exception;
}
