package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class ErrorValidacionException extends OnboardingException {
    public ErrorValidacionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
