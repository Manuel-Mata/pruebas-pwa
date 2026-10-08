package com.proyecto.servicios.exception.onboarding;

public class RfcDuplicadoException extends ClienteYaRegistradoException {
    public RfcDuplicadoException() {
        super("El RFC ya se encuentra registrado en el sistema.");
    }
}
