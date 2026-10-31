package com.proyecto.servicios.exception.onboarding;

import org.springframework.http.HttpStatus;

public class CuentaBloqueadaException extends OnboardingException {
    public CuentaBloqueadaException() {
        super("La cuenta ha sido bloqueada tras 3 intentos fallidos de inicio de sesión.", HttpStatus.FORBIDDEN);
    }
}
