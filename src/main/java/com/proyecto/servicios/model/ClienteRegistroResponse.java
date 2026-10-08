package com.proyecto.servicios.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ClienteRegistroResponse {
    private Long clienteId;
    private String nombreCompleto;
    private String numeroCuenta;
    private String correo;
    private String mensaje;
}
