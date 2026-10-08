package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class ClienteNoEncontradoException extends OnboardingException {
    public ClienteNoEncontradoException() {
        super("El cliente especificado no existe en el sistema.", HttpStatus.NOT_FOUND);
    }
}
