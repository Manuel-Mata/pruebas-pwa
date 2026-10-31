package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class UsuarioInactivoException extends OnboardingException {
    public UsuarioInactivoException() {
        super("El usuario se encuentra inactivo.", HttpStatus.FORBIDDEN);
    }
}
