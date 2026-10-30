package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TransaccionSolicitudRequest {

    @NotNull(message = "El cuentaId es obligatorio")
    @Positive(message = "El cuentaId debe ser mayor a cero")
    private Long cuentaId;

    @NotNull(message = "El idServicio es obligatorio")
    private Integer idServicio;

    @NotNull(message = "El idProducto es obligatorio")
    private Integer idProducto;

    // Teléfono o referencia de pago, según lo que pida el producto en el catálogo (tipoFront).
    @NotBlank(message = "La referencia de pago (teléfono/referencia) es obligatoria")
    private String referenciaPago;

    // Llave de idempotencia provista por quien consume la API (ej. el frontend/app móvil),
    // única por intento de pago. Se usa tal cual como "upc" en el payload hacia GestoPago.
    @NotBlank(message = "El idempotencyKey es obligatorio")
    private String idempotencyKey;
}
