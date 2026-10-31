package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class ClienteYaRegistradoException extends OnboardingException {
    public ClienteYaRegistradoException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
