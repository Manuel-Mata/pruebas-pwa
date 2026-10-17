package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.util.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CuentaEstatusRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = Patrones.NUMERO_CUENTA, message = "El número de cuenta debe tener 10 dígitos")
    private String numeroCuenta;

    @NotNull(message = "El estatus es obligatorio (ACTIVA o INACTIVA)")
    private EstatusCuenta estatus;
}
