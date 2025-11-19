package com.tesseractsoftwares.nexusbackend.sdkjava.config;

public class NexusConfig {
    private String baseUrl;
    private String token = null;
    private int timeoutMS;

    public NexusConfig(String baseUrl, int timeoutMS){
        this.baseUrl = baseUrl;
        this.timeoutMS = timeoutMS;
        this.token = null;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public void clearToken(){
        token = null;
    }
    public String getAuthorizationHeader(){
        if (token == null){
            return null;
        }
        return "Bearer " + token;
    }

    public int getTimeoutMS() {
        return timeoutMS;
    }
    public String getBaseUrl() {
        return baseUrl;
    }
}
