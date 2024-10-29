package com.padaks.todaktodak.common.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${fcm.secret-file}")
    private String secretFileName;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) { // FirebaseApp이 초기화되지 않은 경우에만 초기화
            try {

                String jsonString = getSecretFromAWS(secretFileName);
                // Kubernetes의 Secret에서 파일을 가져옵니다.
                InputStream serviceAccount = getClass().getResourceAsStream("/" + secretFileName);

                if (serviceAccount == null) {
                    throw new IllegalArgumentException("Firebase service account file not found in the specified path");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://padak-todak-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .build();
                FirebaseApp.initializeApp(options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getSecretFromAWS(String secretFileName){

        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().build();
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretFileName);
        GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);

        return getSecretValueResult.getSecretString();
    }
}
