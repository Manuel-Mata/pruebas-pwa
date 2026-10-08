package com.proyecto.servicios.exception.onboarding;

public class CurpDuplicadaException extends ClienteYaRegistradoException {
    public CurpDuplicadaException() {
        super("La CURP ya se encuentra registrada en el sistema.");
    }
}
