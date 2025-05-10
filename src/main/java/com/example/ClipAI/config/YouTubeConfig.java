package com.example.ClipAI.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class YouTubeConfig {
//
//    @Value("${youtube.client-id}")
//    private String clientId;
//
//    @Value("${youtube.client-secret}")
//    private String clientSecret;
//
//    @Value("${youtube.application-name}")
//    private String applicationName;
//
//    @Value("${youtube.tokens-directory-path}")
//    private String tokensDirectoryPath;
//
//    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//    private static final Set<String> SCOPES = YouTubeScopes.all();
//
//
//    @Bean
//    public YouTube youTube() throws GeneralSecurityException, IOException {
//        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
//        Credential credential = getCredentials(httpTransport);
//        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
//                .setApplicationName(applicationName)
//                .build();
//    }
//
//    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
//        // Load client secrets
//        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
//                .setInstalled(new GoogleClientSecrets.Details()
//                        .setClientId(clientId)
//                        .setClientSecret(clientSecret));
//
//        // Build flow and trigger user authorization request
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
//                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
//                .setAccessType("offline")
//                .build();
//
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
//        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//    }

        public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
        public static final NetHttpTransport HTTP_TRANSPORT;

        static {
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize HTTP transport", e);
            }
        }
}
