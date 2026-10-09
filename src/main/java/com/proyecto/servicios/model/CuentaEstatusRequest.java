package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CuentaEstatusRequest {
    @NotNull(message = "El estatus es obligatorio (ACTIVA o INACTIVA)")
    private EstatusCuenta estatus;
}
