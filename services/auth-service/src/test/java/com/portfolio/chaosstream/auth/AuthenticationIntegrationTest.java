package com.portfolio.chaosstream.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static com.portfolio.chaosstream.auth.SecurityConfigConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenGetJwkSet_thenSuccess() throws Exception {
        // Given, When
        this.mockMvc.perform(get(AUTH_KEYS_PATH))
                .andExpect(status().isOk())

                // Then
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    @Test
    void whenGetTokenWithClientCredentials_thenSuccess() throws Exception {
        // Given
        String base64Credentials = Base64.getEncoder().encodeToString((CLIENT_ID + ":secret").getBytes());

        // When
        this.mockMvc.perform(post(AUTH_TOKEN_PATH)
                        .header("Authorization", "Basic " + base64Credentials)
                        .param("grant_type", "client_credentials")
                        .param("scope", MESSAGE_READ_VALUE))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").exists());
    }

    @Test
    void whenGetTokenWithInvalidCredentials_thenUnauthorized() throws Exception {
        // Given
        String base64Credentials = Base64.getEncoder().encodeToString((CLIENT_ID + ":wrong").getBytes());

        // When
        this.mockMvc.perform(post(AUTH_TOKEN_PATH)
                        .header("Authorization", "Basic " + base64Credentials)
                        .param("grant_type", "client_credentials"))

                // Then
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGetTokenWithMissingGrantType_thenBadRequest() throws Exception {
        // Given
        String base64Credentials = Base64.getEncoder().encodeToString((CLIENT_ID + ":secret").getBytes());

        // When
        this.mockMvc.perform(post(AUTH_TOKEN_PATH)
                        .header("Authorization", "Basic " + base64Credentials))

                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenGetTokenWithInvalidGrantType_thenBadRequest() throws Exception {
        // Given
        String base64Credentials = Base64.getEncoder().encodeToString((CLIENT_ID + ":secret").getBytes());

        // When
        this.mockMvc.perform(post(AUTH_TOKEN_PATH)
                        .header("Authorization", "Basic " + base64Credentials)
                        .param("grant_type", "invalid_grant"))

                // Then
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenGetTokenWithoutAuthHeader_thenUnauthorized() throws Exception {
        // When
        this.mockMvc.perform(post(AUTH_TOKEN_PATH)
                        .accept("application/json")
                        .param("grant_type", "client_credentials"))

                // Then
                .andExpect(status().isUnauthorized());
    }
}
