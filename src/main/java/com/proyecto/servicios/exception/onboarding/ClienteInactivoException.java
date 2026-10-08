package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class ClienteInactivoException extends OnboardingException {
    public ClienteInactivoException() {
        super("El cliente se encuentra inactivo y no puede realizar esta operación.", HttpStatus.CONFLICT);
    }
}
