package com.tesseractsoftwares.nexusbackend.sdkjava.auth;

import com.google.gson.Gson;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.AuthResponseDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.LoginDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.auth.dto.RegisterDto;
import com.tesseractsoftwares.nexusbackend.sdkjava.config.NexusConfig;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.NexusHttpClient;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;

public class AuthService {

    private final NexusHttpClient httpClient;
    private final NexusConfig config;
    private final Gson gson = new Gson();

    public AuthService(NexusHttpClient httpClient, NexusConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    // -------------------------
    // REGISTER
    // -------------------------
    public boolean register(RegisterDto dto) throws NexusHttpException {
        String bodyJson = gson.toJson(dto);
        httpClient.post("auth/register", bodyJson);
        return true;
    }

    // -------------------------
    // LOGIN
    // -------------------------
    public String login(LoginDto dto) throws NexusHttpException {
        String bodyJson = gson.toJson(dto);
        String response = httpClient.post("auth/login", bodyJson);

        AuthResponseDto parsed = gson.fromJson(response, AuthResponseDto.class);
        String token = parsed.getToken();

        if (token != null) {
            config.setToken(token);
        }

        return token;
    }

    // -------------------------
    // VALIDATE
    // -------------------------
    public boolean validate() throws NexusHttpException {
        String response = httpClient.post("auth/validate", "{}");

        if (response == null) return false;

        response = response.toLowerCase();

        return response.contains("success") || response.contains("true");
    }

    // -------------------------
    // LOGOUT
    // -------------------------
    public boolean logout() throws NexusHttpException {
        httpClient.post("auth/logout", "{}");
        config.clearToken();
        return true;
    }

    // -------------------------
    // REFRESH TOKEN
    // -------------------------
    public String refresh() throws NexusHttpException {
        String response = httpClient.post("auth/refresh", "{}");

        AuthResponseDto parsed = gson.fromJson(response, AuthResponseDto.class);
        String newToken = parsed.getToken();

        if (newToken != null) {
            config.setToken(newToken);
        }

        return newToken;
    }
}
