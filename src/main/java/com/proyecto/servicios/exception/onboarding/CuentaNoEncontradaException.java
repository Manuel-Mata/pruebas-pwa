package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends OnboardingException {
    public CuentaNoEncontradaException() {
        super("La cuenta especificada no existe en el sistema.", HttpStatus.NOT_FOUND);
    }
}
