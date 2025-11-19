package com.tesseractsoftwares.nexusbackend.sdkjava.http;

import com.tesseractsoftwares.nexusbackend.sdkjava.config.NexusConfig;
import com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions.NexusHttpException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NexusHttpClient {

    private final NexusConfig config;

    public NexusHttpClient(NexusConfig config) {
        this.config = config;
    }

    public String post(String endpoint, String jsonBody) throws NexusHttpException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(config.getBaseUrl() + endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(config.getTimeoutMS());
            connection.setReadTimeout(config.getTimeoutMS());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Authorization header if token exists
            String authHeader = config.getAuthorizationHeader();
            if (authHeader != null) {
                connection.setRequestProperty("Authorization", authHeader);
            }

            // Send JSON body
            if (jsonBody != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Response handling
            int statusCode = connection.getResponseCode();
            InputStream responseStream = (statusCode >= 200 && statusCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(responseStream);

            if (statusCode >= 200 && statusCode < 300) {
                return response;
            } else {
                throw new NexusHttpException(
                        "HTTP error",
                        statusCode,
                        endpoint,
                        response
                );
            }

        } catch (Exception e) {
            throw new NexusHttpException(
                    "Request failed: " + e.getMessage(),
                    0,
                    endpoint,
                    null
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        return result.toString();
    }
    public String get(String endpoint) throws NexusHttpException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(config.getBaseUrl() + endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(config.getTimeoutMS());
            connection.setReadTimeout(config.getTimeoutMS());
            connection.setRequestProperty("Content-Type", "application/json");

            String auth = config.getAuthorizationHeader();
            if (auth != null) connection.setRequestProperty("Authorization", auth);

            int status = connection.getResponseCode();
            InputStream stream = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(stream);

            if (status >= 200 && status < 300) return response;

            throw new NexusHttpException("HTTP error", status, endpoint, response);

        } catch (Exception e) {
            throw new NexusHttpException("Request failed: " + e.getMessage(), 0, endpoint, null);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }


    public String put(String endpoint, String jsonBody) throws NexusHttpException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(config.getBaseUrl() + endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setConnectTimeout(config.getTimeoutMS());
            connection.setReadTimeout(config.getTimeoutMS());
            connection.setRequestProperty("Content-Type", "application/json");

            String auth = config.getAuthorizationHeader();
            if (auth != null) connection.setRequestProperty("Authorization", auth);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            InputStream stream = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(stream);

            if (status >= 200 && status < 300) return response;

            throw new NexusHttpException("HTTP error", status, endpoint, response);

        } catch (Exception e) {
            throw new NexusHttpException("Request failed: " + e.getMessage(), 0, endpoint, null);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }


    public String delete(String endpoint) throws NexusHttpException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(config.getBaseUrl() + endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("DELETE");
            connection.setConnectTimeout(config.getTimeoutMS());
            connection.setReadTimeout(config.getTimeoutMS());
            connection.setRequestProperty("Content-Type", "application/json");

            String auth = config.getAuthorizationHeader();
            if (auth != null) connection.setRequestProperty("Authorization", auth);

            int status = connection.getResponseCode();
            InputStream stream = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(stream);

            if (status >= 200 && status < 300) return response;

            throw new NexusHttpException("HTTP error", status, endpoint, response);

        } catch (Exception e) {
            throw new NexusHttpException("Request failed: " + e.getMessage(), 0, endpoint, null);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
