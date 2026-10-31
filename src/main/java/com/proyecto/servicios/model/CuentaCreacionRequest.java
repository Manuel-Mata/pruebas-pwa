package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CuentaCreacionRequest {
    @NotNull(message = "El clienteId es obligatorio")
    @Positive(message = "El clienteId debe ser mayor a cero")
    private Long clienteId;
}
