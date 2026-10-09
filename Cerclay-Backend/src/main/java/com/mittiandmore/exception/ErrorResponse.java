package com.mittiandmore.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {

    private String code;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;
    private String path;

    public ErrorResponse() {}

    public ErrorResponse(
        String code,
        String message,
        Map<String, String> errors,
        LocalDateTime timestamp,
        String path
    ) {
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.timestamp = timestamp;
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
