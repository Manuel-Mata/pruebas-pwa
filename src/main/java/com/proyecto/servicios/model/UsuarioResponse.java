package com.proyecto.servicios.model;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class UsuarioResponse {
    private Long id;
    private Long clienteId;
    private String correo;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
