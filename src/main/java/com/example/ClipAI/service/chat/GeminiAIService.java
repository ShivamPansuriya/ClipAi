//package com.example.ClipAI.service;
//
//import com.example.ClipAI.model.ClipAIRest;
//
//import com.example.ClipAI.model.GeminiResponse;
//import com.example.ClipAI.model.Image;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import okhttp3.HttpUrl;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//import org.json.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.util.Objects;
//import java.util.concurrent.atomic.AtomicReference;
//
//@Service
//public class GeminiAIService {
//    private static final String GEMINI_URL =
//            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
//    private static final String API_KEY = "AIzaSyA9dogKPtbxw-OaDbK8qQlRUww1J4D7Kco";
//    private final OkHttpClient client = new OkHttpClient();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final JSONParser jsonParser = new JSONParser();
//    private final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);
//
//    public void generateScript(ClipAIRest clipAIRest) {
//        try {
//            // Prepare JSON payload
//            String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}",
//                    generateScriptPrompt(clipAIRest));
//            Response response = getResponse(GEMINI_URL, jsonPayload);
//
//            AtomicReference<String> script = new AtomicReference<>();
//
//            if (response.body() != null) {
//                String responseBody = response.body().string();
//                GeminiResponse root = objectMapper.readValue(responseBody, GeminiResponse.class);
//
//                // Extract prompts from mapped object
//                root.getCandidates().forEach(candidate -> candidate.getContent().getParts()
//                        .forEach(part -> script.set(part.getText().replaceAll("\"[^\"]*\"", ""))));
//            }
//            clipAIRest.setScript(script.get());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void generateImagesPrompts(ClipAIRest clipAIRest) {
//        try {
//            // Prepare JSON payload
//            String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}",
//                    generateImagesPrompt(clipAIRest));
//            Response response = getResponse(GEMINI_URL, jsonPayload);
//
//            if (response.body() != null) {
//                String responseBody = response.body().string();
//                GeminiResponse root = objectMapper.readValue(responseBody, GeminiResponse.class);
//
//                // Extract prompts from mapped object
//                root.getCandidates().forEach(candidate -> candidate.getContent().getParts().forEach(part -> {
//                    String imageJsonString = part.getText();
//                    imageJsonString = imageJsonString.replace("```json", "").replace("```", "").trim();
//
//                    // Parse the inner JSON string to get the image descriptions
//                    JSONObject imageDescriptions = new JSONObject(imageJsonString);
//                    for (String key : imageDescriptions.keySet()) {
//                        Image image = new Image();
//                        image.setKey(key);
//                        image.setDescription(imageDescriptions.getString(key));
//                        clipAIRest.addImage(image);
//                    }
//                }));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private String generateScriptPrompt(ClipAIRest clipAIRest) {
//        return String.format("give me script on topic %s:\n" + "\n" + "sample of script of other topic: \n"
//                        + "Imagine a world shrouded in perpetual darkness, where pressure crushes bone, and strange, bioluminescent creatures dance in the abyss. This is the deep ocean, a vast and unexplored realm teeming with secrets. We've only mapped a fraction of its depths, leaving countless mysteries hidden beneath the waves. Imagine colossal squid lurking in the shadows, hydrothermal vents spewing scorching water, and fish with translucent bodies and glowing teeth. This is a world where evolution has sculpted bizarre life forms, adapted to survive in extreme conditions. The deep ocean holds the key to understanding life on Earth, revealing the resilience and adaptability of life itself. Join us as we dive into the mysteries of the deep, uncovering the secrets that lie hidden beneath the surface.\n"
//                        + "\n" + "generate 40-50 seconds script based on example, include some audience holding facts that  makes audience to more video.give direct script do not give extra information like narrator/speaker etc and quoted words as it create problem in json creation.only give script",
//                clipAIRest.getTopic());
//    }
//
//    private String generateImagesPrompt(ClipAIRest clipAIRest) {
//        return String.format(
//                "%s \n based on this script of %s. give me 15 multiple high resolution, realistic image prompt so that i can keep it as my video background and can do voice over as per narration.give me more detail image with some real object which attract viewers to view more of video. \n return result in json array containing result of images like result:[key:{image prompt}]. in this give format key is some key words from the script based on which image is being generated. only give json result do not include extra result",
//                clipAIRest.getScript(), clipAIRest.getTopic());
//    }
//
//    private Response getResponse(String baseUrl, String jsonPayload) throws IOException {
//        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(baseUrl)).newBuilder();
//        urlBuilder.addQueryParameter("key", API_KEY);
//
//        // Create request
//        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));
//        Request request = new Request.Builder().url(urlBuilder.build().toString())
//                .addHeader("Content-Type", "application/json").post(body).build();
//
//        // Execute request
//        Response response = client.newCall(request).execute();
//
//        if (!response.isSuccessful()) {
//            logger.error("unable to read response fom gemini ai");
//            throw new IOException("Unexpected response code: " + response);
//        }
//
//        return response;
//    }
//}
