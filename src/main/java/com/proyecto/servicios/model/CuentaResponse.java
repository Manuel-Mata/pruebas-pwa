package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstatusCuenta;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Builder
public class CuentaResponse {
    private String numeroCuenta;
    private BigDecimal saldo;
    private EstatusCuenta estatus;
    private OffsetDateTime fechaApertura;
    private Long clienteId;
}
