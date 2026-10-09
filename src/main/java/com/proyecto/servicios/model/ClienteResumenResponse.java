package com.proyecto.servicios.model;

import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;

@Getter @Builder
public class ClienteResumenResponse {
    private Long id;
    private String nombreCompleto;
    private String curp;
    private String rfc;
    private String correo;
    private String telefonoMovil;
    private Boolean activo;
    private OffsetDateTime fechaRegistro;
}
