package com.tesseractsoftwares.nexusbackend.sdkjava.http.exceptions;

public class NexusHttpException extends Exception {

    private final int statusCode;
    private final String endpoint;
    private final String errorBody;

    public NexusHttpException(String message, int statusCode, String endpoint, String errorBody) {
        super(message);
        this.statusCode = statusCode;
        this.endpoint = endpoint;
        this.errorBody = errorBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getErrorBody() {
        return errorBody;
    }
}
