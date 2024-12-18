package com.example.ClipAI.service.chat;

import com.example.ClipAI.model.ClipAIRest;
import com.example.ClipAI.model.GeminiResponse;
import com.example.ClipAI.model.Image;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractGeminiService implements GeminiService{
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
//    private static final String API_KEY = "AIzaSyA9dogKPtbxw-OaDbK8qQlRUww1J4D7Kco";
    private static final String API_KEY = "AIzaSyA2OkjubYIKKz_URfe0PokvKPwp0dZ8VtQ";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(AbstractGeminiService.class);
    private final OkHttpClient client =
            new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

    protected abstract String getPrompt(ClipAIRest clipAIRest);

    @Override
    public void generateScript(ClipAIRest clipAIRest) {
        try {
            // Prepare JSON payload
            String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}",
                    getPrompt(clipAIRest));
            Response response = getResponse(GEMINI_URL, jsonPayload);

            AtomicReference<String> script = new AtomicReference<>();

            if (response.body() != null) {
                String responseBody = response.body().string();
                GeminiResponse root = objectMapper.readValue(responseBody, GeminiResponse.class);

                // Extract prompts from mapped object
                root.getCandidates().forEach(candidate -> candidate.getContent().getParts()
                        .forEach(part -> script.set(part.getText().replaceAll("\"[^\"]*\"", ""))));
            }
            clipAIRest.setScript(script.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("script:{}",clipAIRest.getScript());
    }

    @Override
    public void generateImagesPrompts(ClipAIRest clipAIRest) {
        try {
            // Prepare JSON payload
            String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\": \"%s\"}]}]}",
                    getImagesPrompt(clipAIRest));
            JSONObject object = new JSONObject(jsonPayload);
            Response response = getResponse(GEMINI_URL, jsonPayload);

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
                        logger.error("json result text:{} ,", imageJsonString,e);
                    }
                }));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Response getResponse(String baseUrl, String jsonPayload) throws IOException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(baseUrl)).newBuilder();
        urlBuilder.addQueryParameter("key", API_KEY);

        // Create request
        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(urlBuilder.build().toString())
                .addHeader("Content-Type", "application/json").post(body).build();

        // Execute request
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            logger.error("unable to read response fom gemini ai");
            throw new IOException("Unexpected response code: " + response);
        }

        return response;
    }
}
