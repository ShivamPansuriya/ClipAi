package com.example.ClipAI.service.audio;

import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.audio.TranscriptionResult;

public interface AudioService {
    public TranscriptionResult transcribeAudio(String audioPath, ClipAIRest clipAIRest) throws Exception;
    public TranscriptionResult transcribeAudio(String audioPath, AudioServiceImpl.WhisperModel model, String language, ClipAIRest clipAIRest) throws Exception;
}
