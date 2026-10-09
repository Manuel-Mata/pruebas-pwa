package com.proyecto.servicios.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CuentaCreacionRequest {
    @NotNull(message = "El clienteId es obligatorio")
    @Positive(message = "El clienteId debe ser mayor a cero")
    private Long clienteId;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El saldo admite hasta 12 enteros y 2 decimales")
    private BigDecimal saldoInicial;
}
