package com.proyecto.servicios.exception.onboarding;

public class CorreoDuplicadoException extends ClienteYaRegistradoException {
    public CorreoDuplicadoException() {
        super("El correo electrónico ya se encuentra registrado en el sistema.");
    }
}
