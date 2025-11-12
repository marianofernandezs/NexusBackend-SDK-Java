package com.prax.core.velocity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * BackendClient usando HttpURLConnection (sin dependencias externas)
 * Compatible con cualquier versión de Java y funciona perfectamente en Docker
 */
public class BackendClient {

    private static final Logger logger = LoggerFactory.getLogger(BackendClient.class);
    private static final String BASE_URL = "http://praxsuite_backend:3000";
    private static final int CONNECT_TIMEOUT = 10000; // 10 segundos
    private static final int READ_TIMEOUT = 30000;    // 30 segundos

    // ==============================
    // 🔹 Registrar nuevo usuario
    // ==============================
    public boolean register(UUID uuid, String email, String password, String birthdate, String playerName) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", uuid.toString());
            payload.addProperty("email", email);
            payload.addProperty("password", password);
            payload.addProperty("birthdate", birthdate);
            payload.addProperty("player_name", playerName);

            logger.info("[Backend] Enviando payload de registro: {}", maskPassword(payload));

            String response = sendPostRequest("/api/auth/register", payload.toString(), null);
            logger.info("[Backend] Registro response: {}", response);

            return response != null && !response.isEmpty();

        } catch (Exception e) {
            logger.error("[Backend] Error en registro: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==============================
    // 🔹 Iniciar sesión (login)
    // ==============================
    public Optional<String> login(UUID uuid, String email, String password) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", uuid.toString());
            payload.addProperty("email", email);
            payload.addProperty("password", password);

            logger.info("[Backend] Intentando login para: {}", email);

            String response = sendPostRequest("/api/auth/login", payload.toString(), null);
            logger.info("[Backend] Login response: {}", response);

            if (response != null && !response.isEmpty()) {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                if (json.has("token")) {
                    return Optional.of(json.get("token").getAsString());
                }
            }

        } catch (Exception e) {
            logger.error("[Backend] Error en login: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ==============================
    // 🔹 Validar token
    // ==============================
    public boolean validate(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("[Backend] Token nulo o vacío en validación");
            return false;
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("token", token);

            String response = sendPostRequest("/api/auth/validate", payload.toString(), token);
            logger.info("[Backend] Validate response: {}", response);

            return response != null && !response.isEmpty();

        } catch (Exception e) {
            logger.error("[Backend] Error validando token: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==============================
    // 🔹 Cerrar sesión
    // ==============================
    public boolean logout(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("[Backend] Token nulo o vacío en logout");
            return false;
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("token", token);

            String response = sendPostRequest("/api/auth/logout", payload.toString(), token);
            logger.info("[Backend] Logout response: {}", response);

            return response != null && !response.isEmpty();

        } catch (Exception e) {
            logger.error("[Backend] Error al cerrar sesión: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==============================
    // 🔹 Refrescar token
    // ==============================
    public Optional<String> refresh(String oldToken) {
        if (oldToken == null || oldToken.isEmpty()) {
            logger.warn("[Backend] Token nulo o vacío en refresh");
            return Optional.empty();
        }

        try {
            String response = sendPostRequest("/api/auth/refresh", "", oldToken);
            logger.info("[Backend] Refresh response: {}", response);

            if (response != null && !response.isEmpty()) {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                if (json.has("token")) {
                    return Optional.of(json.get("token").getAsString());
                }
            }

        } catch (Exception e) {
            logger.error("[Backend] Error al refrescar token: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ==============================
    // 🔹 Método privado para enviar POST
    // ==============================
    private String sendPostRequest(String endpoint, String jsonBody, String bearerToken) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // Crear URL
            URL url = new URL(BASE_URL + endpoint);
            logger.debug("[Backend] Conectando a: {}", url.toString());

            // Abrir conexión
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // Agregar token si existe
            if (bearerToken != null && !bearerToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }

            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Enviar body si no está vacío
            if (jsonBody != null && !jsonBody.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }
            }

            // Leer respuesta
            int responseCode = connection.getResponseCode();
            logger.debug("[Backend] Response code: {}", responseCode);

            // Decidir qué stream leer (error o normal)
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // Retornar null si el código no es exitoso
            if (responseCode < 200 || responseCode >= 300) {
                logger.warn("[Backend] Error response ({}): {}", responseCode, response.toString());
                return null;
            }

            return response.toString();

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    logger.debug("[Backend] Error cerrando reader: {}", e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ==============================
    // 🔹 Utilidad para enmascarar password
    // ==============================
    private String maskPassword(JsonObject payload) {
        JsonObject masked = new JsonObject();
        payload.entrySet().forEach(entry -> {
            if (entry.getKey().equals("password")) {
                masked.addProperty(entry.getKey(), "***");
            } else {
                masked.add(entry.getKey(), entry.getValue());
            }
        });
        return masked.toString();
    }
}