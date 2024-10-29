package com.padaks.todaktodak.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import javax.annotation.PostConstruct;
import java.io.IOException;
import com.google.common.collect.Lists;

@Configuration
public class FcmConfig {

    @Value("${fcm.secret-file}")
    private String secretFileName;

    @PostConstruct
    public void initialize() {
        try {
            // Load the Firebase service account JSON from the classpath
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ClassPathResource(secretFileName).getInputStream())
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .build();

            // Initialize Firebase app only once
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            System.out.println("FCM SETTING SUCCESS");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize FCM: " + e.getMessage());
        }
    }
}
