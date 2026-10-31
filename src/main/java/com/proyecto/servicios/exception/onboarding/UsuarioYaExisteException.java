package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class UsuarioYaExisteException extends OnboardingException {
    public UsuarioYaExisteException() {
        super("El cliente ya tiene un usuario de acceso asociado.", HttpStatus.CONFLICT);
    }
}
