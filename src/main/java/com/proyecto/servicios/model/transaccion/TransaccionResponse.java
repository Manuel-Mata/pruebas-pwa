package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.enums.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionResponse {
    private Long id;
    private String upc;
    private EstadoTransaccion estado;
    private BigDecimal monto;
    private String idTxGestoPago;
    private String numAutorizacion;
    private String mensaje;
    private LocalDateTime fechaSolicitud;
}
