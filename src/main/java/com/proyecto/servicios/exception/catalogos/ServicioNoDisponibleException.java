package com.proyecto.servicios.exception.catalogos;

import com.proyecto.servicios.exception.onboarding.OnboardingException;

import org.springframework.http.HttpStatus;

public class ServicioNoDisponibleException extends OnboardingException {
    public ServicioNoDisponibleException() {
        super("Servicio de validación postal temporalmente fuera de línea.", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
