package com.example.ClipAI.util;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for HTTP operations.
 * Provides methods for making HTTP requests.
 */
@Component
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    
    private final OkHttpClient client;
    
    public HttpUtils() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(240, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
    
    /**
     * Makes a POST request with JSON payload.
     *
     * @param url The URL to send the request to
     * @param jsonPayload The JSON payload to send
     * @param headers The headers to include in the request
     * @return The response from the server
     * @throws IOException If an I/O error occurs
     */
    public Response postJson(String url, String jsonPayload, Headers headers) throws IOException {
        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .post(body)
                .build();
        
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            logger.error("HTTP request failed with code: {}", response.code());
            String responseBody = response.body() != null ? response.body().string() : "No response body";
            response.close();
            throw new IOException("Unexpected response code: " + response.code() + ", body: " + responseBody);
        }
        
        return response;
    }
    
    /**
     * Makes a GET request.
     *
     * @param url The URL to send the request to
     * @param headers The headers to include in the request
     * @return The response from the server
     * @throws IOException If an I/O error occurs
     */
    public Response get(String url, Headers headers) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .get()
                .build();
        
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            logger.error("HTTP request failed with code: {}", response.code());
            String responseBody = response.body() != null ? response.body().string() : "No response body";
            response.close();
            throw new IOException("Unexpected response code: " + response.code() + ", body: " + responseBody);
        }
        
        return response;
    }
    
    /**
     * Creates Headers object from key-value pairs.
     *
     * @param keyValues Key-value pairs for headers (must be even number of arguments)
     * @return Headers object
     */
    public Headers createHeaders(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Headers must be provided as key-value pairs");
        }
        
        Headers.Builder builder = new Headers.Builder();
        for (int i = 0; i < keyValues.length; i += 2) {
            builder.add(keyValues[i], keyValues[i + 1]);
        }
        
        return builder.build();
    }
}
