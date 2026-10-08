package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends OnboardingException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos.", HttpStatus.UNAUTHORIZED);
    }
}
