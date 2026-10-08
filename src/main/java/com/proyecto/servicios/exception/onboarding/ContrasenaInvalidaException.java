package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class ContrasenaInvalidaException extends OnboardingException {
    public ContrasenaInvalidaException() {
        super("La contraseña no cumple con los requisitos mínimos de seguridad.", HttpStatus.BAD_REQUEST);
    }
}
