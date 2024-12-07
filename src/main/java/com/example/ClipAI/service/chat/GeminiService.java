package com.example.ClipAI.service.chat;

import com.example.ClipAI.model.ClipAIRest;

public interface GeminiService {
    public void generateScript(ClipAIRest clipAIRest);
    public String getImagesPrompt(ClipAIRest clipAIRest);
    public void generateImagesPrompts(ClipAIRest clipAIRest);
}
