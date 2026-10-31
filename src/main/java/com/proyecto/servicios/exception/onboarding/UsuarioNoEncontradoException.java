package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class UsuarioNoEncontradoException extends OnboardingException {
    public UsuarioNoEncontradoException() {
        super("El usuario especificado no existe en el sistema.", HttpStatus.NOT_FOUND);
    }
}
