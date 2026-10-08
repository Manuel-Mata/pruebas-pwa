package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public abstract class OnboardingException extends RuntimeException {
    private final HttpStatus status;

    public OnboardingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
