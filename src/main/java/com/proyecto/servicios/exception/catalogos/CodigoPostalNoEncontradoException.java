package com.proyecto.servicios.exception.catalogos;

import com.proyecto.servicios.exception.onboarding.OnboardingException;

import org.springframework.http.HttpStatus;

public class CodigoPostalNoEncontradoException extends OnboardingException {
    public CodigoPostalNoEncontradoException() {
        super("El código postal no existe en el catálogo.", HttpStatus.NOT_FOUND);
    }
}
