package com.example.ClipAI.service.chat;

import com.example.ClipAI.config.AppConfig;
import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.GeminiResponse;
import com.example.ClipAI.model.Image;
import com.example.ClipAI.model.youtube.VideoRequest;
import com.example.ClipAI.util.HttpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for Gemini AI services.
 * Provides common functionality for interacting with the Gemini API.
 */
public abstract class AbstractGeminiServiceRefactored implements GeminiService {
    private final Logger logger = LoggerFactory.getLogger(AbstractGeminiServiceRefactored.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    protected AppConfig appConfig;
    
    @Autowired
    protected HttpUtils httpUtils;
    
    /**
     * Gets metadata prompt for the given ClipAIRest object.
     * To be implemented by subclasses.
     *
     * @param clipAIRest The ClipAIRest object
     * @return The metadata prompt
     */
    protected abstract String getMetaData(ClipAIRest clipAIRest);
    
    @Override
    public void generateScript(ClipAIRest clipAIRest, ChatType chatType) {
        if (clipAIRest.getScript() == null) {
            try {
                // Prepare JSON payload
                String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}",
                        chatType.getScript().formatted(clipAIRest.getTopic()));
                
                // Create headers
                Headers headers = httpUtils.createHeaders(
                        "Content-Type", "application/json"
                );
                
                // Execute request
                Response response = httpUtils.postJson(
                        appConfig.getGeminiUrl() + "?key=" + appConfig.getGeminiApiKey(),
                        jsonPayload,
                        headers
                );
                
                AtomicReference<String> script = new AtomicReference<>();
                
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    GeminiResponse root = objectMapper.readValue(responseBody, GeminiResponse.class);
                    
                    // Extract prompts from mapped object
                    root.getCandidates().forEach(candidate -> candidate.getContent().getParts()
                            .forEach(part -> script.set(part.getText().replaceAll("\"[^\"]*\"", ""))));
                }
                clipAIRest.setScript(script.get());
                response.close();
            } catch (IOException e) {
                logger.error("Error generating script: {}", e.getMessage(), e);
            }
            logger.info("Generated script: {}", clipAIRest.getScript());
        }
    }
    
    @Override
    public void generateImagesPrompts(ClipAIRest clipAIRest) {
        try {
            // Prepare JSON payload
            String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}",
                    getImagesPrompt(clipAIRest));
            
            // Create headers
            Headers headers = httpUtils.createHeaders(
                    "Content-Type", "application/json"
            );
            
            // Execute request
            Response response = httpUtils.postJson(
                    appConfig.getGeminiUrl() + "?key=" + appConfig.getGeminiApiKey(),
                    jsonPayload,
                    headers
            );
            
            if (response.body() != null) {
                String responseBody = response.body().string();
                GeminiResponse root = objectMapper.readValue(responseBody, GeminiResponse.class);
                
                // Extract prompts from mapped object
                root.getCandidates().forEach(candidate -> candidate.getContent().getParts().forEach(part -> {
                    String imageJsonString = part.getText();
                    try {
                        imageJsonString = imageJsonString.replace("```json", "").replace("```", "").trim();
                        
                        // Parse the inner JSON string to get the image descriptions
                        JSONObject imageDescriptions = new JSONObject(imageJsonString);
                        JSONArray resultArray = imageDescriptions.getJSONArray("result");
                        for (int i = 0; i < resultArray.length(); i++) {
                            JSONObject imageObject = resultArray.getJSONObject(i);
                            String key = imageObject.keys().next(); // Get the first (and only) key
                            
                            Image image = new Image();
                            image.setKey(key);
                            image.setDescription(imageObject.getString(key));
                            image.setWidth(clipAIRest.getImageWidth());
                            image.setHeight(clipAIRest.getImageHeight());
                            clipAIRest.addImage(image);
                        }
                    } catch (JSONException e) {
                        logger.error("Error parsing JSON result: {}", imageJsonString, e);
                    }
                }));
            }
            response.close();
        } catch (IOException e) {
            logger.error("Error generating image prompts: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Filters images based on the script content.
     *
     * @param clipAIRest The ClipAIRest object containing images and script
     */
    public void filterImages(ClipAIRest clipAIRest) {
        String filterScript = clipAIRest.getScript().replaceAll("[^a-zA-Z\\s]", "").toLowerCase();
        List<String> originalScriptWords = Arrays.stream(filterScript.split("\\s+")).toList();
        for (var image : clipAIRest.getImages()) {
            String imageName = image.getKey().toLowerCase();
            imageName = imageName.replace('_', ' ').toLowerCase();
            if (!filterScript.contains(imageName)) {
                String[] imageNameParts = imageName.split(" ");
                for (var part : imageNameParts) {
                    if (originalScriptWords.contains(part)) {
                        int partIndex = originalScriptWords.indexOf(part);
                        if (partIndex + 2 < originalScriptWords.size()) {
                            image.setKey(String.format("%s %s %s",
                                    originalScriptWords.get(partIndex), originalScriptWords.get(partIndex + 1),
                                    originalScriptWords.get(partIndex + 2)));
                        }
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Generates metadata for a video request.
     *
     * @param clipAIRest The ClipAIRest object
     * @param videoRequest The VideoRequest to populate with metadata
     * @return The populated VideoRequest
     */
    public VideoRequest generateMetaData(ClipAIRest clipAIRest, VideoRequest videoRequest) {
        String tags = "#facts #knowledge #viral #new #latest #trending #top #fact #youtubeshorts #viralshortvideo #trendingfacts #ytshorts #amazing";
        if (clipAIRest.getScript() != null) {
            try {
                // Prepare JSON payload
                String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}",
                        getMetaData(clipAIRest));
                
                // Create headers
                Headers headers = httpUtils.createHeaders(
                        "Content-Type", "application/json"
                );
                
                // Execute request
                Response response = httpUtils.postJson(
                        appConfig.getGeminiUrl() + "?key=" + appConfig.getGeminiApiKey(),
                        jsonPayload,
                        headers
                );
                
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    GeminiResponse root = objectMapper.readValue(responseBody, GeminiResponse.class);
                    
                    // Extract prompts from mapped object
                    root.getCandidates().forEach(candidate -> candidate.getContent().getParts()
                            .forEach(part -> {
                                String imageJsonString = part.getText().replace("```json", "").replace("```", "").trim();
                                
                                try {
                                    // Parse the inner JSON string to get the image descriptions
                                    JSONObject imageDescriptions = new JSONObject(imageJsonString);
                                    JSONArray resultArray = imageDescriptions.getJSONArray("result");
                                    JSONObject metaData = resultArray.getJSONObject(0);
                                    videoRequest.setDescription(metaData.getString("description") + " " + tags);
                                    videoRequest.setTags(Arrays.asList(metaData.getString("tags").split(",")));
                                    videoRequest.setTitle(metaData.getString("title"));
                                } catch (JSONException e) {
                                    logger.error("Error parsing metadata JSON: {}", imageJsonString, e);
                                }
                            }));
                }
                response.close();
            } catch (IOException e) {
                logger.error("Error generating metadata: {}", e.getMessage(), e);
            }
        }
        return videoRequest;
    }
}
