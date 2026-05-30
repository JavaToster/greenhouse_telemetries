package com.example.greenhouse_telemetries.exceptions.auth;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(String message) {
        super(message);
    }
}
