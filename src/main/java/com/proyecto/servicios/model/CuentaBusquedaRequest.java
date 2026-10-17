package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.util.Patrones;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CuentaBusquedaRequest {

    @Pattern(regexp = Patrones.NUMERO_CUENTA, message = "El número de cuenta debe tener 10 dígitos")
    private String numeroCuenta;

    @Positive(message = "El clienteId debe ser mayor a cero")
    private Long clienteId;

    private EstatusCuenta estatus;
}
