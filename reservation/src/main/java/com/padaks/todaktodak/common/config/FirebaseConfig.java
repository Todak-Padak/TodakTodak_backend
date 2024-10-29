package com.padaks.todaktodak.common.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${fcm.secret-file}")
    private String secretFileName;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) { // FirebaseApp이 초기화되지 않은 경우에만 초기화
            try {
                ClassLoader classLoader = getClass().getClassLoader();
//                InputStream serviceAccount = classLoader.getResourceAsStream("todak-1f8d0-firebase-adminsdk-tbqa8-b7c41789c9.json");

                String jsonString = getSecretFromAWS(secretFileName);
                // Kubernetes의 Secret에서 파일을 가져옵니다.
                InputStream serviceAccount = getClass().getResourceAsStream("/" + secretFileName);

                if (serviceAccount == null) {
                    throw new IllegalArgumentException("Firebase service account file not found in the specified path");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://todak-1f8d0-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .build();
                FirebaseApp.initializeApp(options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getSecretFromAWS(String secretFileName){
        log.info(secretFileName);
        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().build();
        log.info(client.toString());
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretFileName);
        log.info(getSecretValueRequest.getSecretId());
        GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);
        log.info(getSecretValueResult.getSecretString());

        return getSecretValueResult.getSecretString();
    }
}
