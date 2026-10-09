package com.mittiandmore.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${google.client-id:}')")
public class GoogleAuthConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(@Value("${google.client-id:}") String googleClientId)
        throws GeneralSecurityException, IOException {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID environment variable is not configured");
        }

        return new GoogleIdTokenVerifier.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance()
        )
            .setAudience(Collections.singletonList(googleClientId))
            .build();
    }
}
