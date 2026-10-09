package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class LoginResponse extends GenericResponse {
    private String token;
    private String tipo;
    private long expiraEnSegundos;
}
