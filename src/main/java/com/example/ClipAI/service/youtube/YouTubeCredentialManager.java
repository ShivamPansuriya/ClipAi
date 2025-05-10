package com.example.ClipAI.service.youtube;

import com.example.ClipAI.config.YouTubeConfig;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTubeScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Service
@Slf4j
public class YouTubeCredentialManager {

    private static final Set<String> SCOPES = YouTubeScopes.all();

    private String tokensDirectoryPath;

    @Value("${youtube.client-id}")
    private String clientId;

    @Value("${youtube.client-secret}")
    private String clientSecret;

    private final FileDataStoreFactory dataStoreFactory;

    public YouTubeCredentialManager(@Value("${youtube.tokens-directory-path}") String tokensDirectoryPath) throws IOException {
        this.tokensDirectoryPath = tokensDirectoryPath;
        this.dataStoreFactory = new FileDataStoreFactory(new File(tokensDirectoryPath));
    }

    private void storeCredential(String userId, String accessToken, String refreshToken) throws IOException {
        DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore("credentials");
        StoredCredential storedCredential = new StoredCredential();
        storedCredential.setAccessToken(accessToken);
        storedCredential.setRefreshToken(refreshToken);
        dataStore.set(userId, storedCredential);
    }

    public Credential createCredential(long userId) throws IOException {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                YouTubeConfig.HTTP_TRANSPORT,
                YouTubeConfig.JSON_FACTORY,
                clientSecrets,
                SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize(String.valueOf(userId));

        // Store both access and refresh tokens
        storeCredential(String.valueOf(userId), credential.getAccessToken(), credential.getRefreshToken());

        return flow.loadCredential(String.valueOf(userId));
    }

    public Credential getStoredCredential(long userId) throws IOException {
        DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore("credentials");
        StoredCredential stored = dataStore.get(String.valueOf(userId));

        if (stored == null) {
            log.info("No stored credential found for user: {}. Need to perform OAuth2 flow.", userId);
            return null;
        }

        // Create flow for token refresh
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                YouTubeConfig.HTTP_TRANSPORT,
                YouTubeConfig.JSON_FACTORY,
                clientSecrets,
                SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .build();

        // Create credential with stored tokens
        return flow.createAndStoreCredential(
                new TokenResponse()
                        .setAccessToken(stored.getAccessToken())
                        .setRefreshToken(stored.getRefreshToken()),
                String.valueOf(userId));
    }
}
