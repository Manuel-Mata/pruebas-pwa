package com.proyecto.servicios.entity.transacciones;

import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    // Llave de idempotencia de punta a punta: la generamos nosotros, se guarda
    // única en esta tabla y es el mismo valor que se envía a GestoPago en el
    // campo "upc" del payload (su propia ventana de deduplicación: 15 min para
    // recargas/entretenimiento, 24h para pagos con referencia).
    @Column(name = "upc", nullable = false, unique = true, length = 100)
    private String upc;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    // Teléfono o referencia, según lo que pida el tipoFront del producto en el catálogo.
    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    // JSON exacto (sin encriptar) enviado en sendTx.do. Se guarda para poder reenviar
    // un payload idéntico al reconciliar con confirmTx.do (mismo upc y datos).
    @Column(name = "payload_original", nullable = false, columnDefinition = "TEXT")
    private String payloadOriginal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoTransaccion estado = EstadoTransaccion.PENDIENTE;

    @Column(name = "id_tx_gestopago", length = 50)
    private String idTxGestoPago;

    @Column(name = "num_autorizacion", length = 50)
    private String numAutorizacion;

    @Column(name = "saldo_resultante", precision = 14, scale = 2)
    private BigDecimal saldoResultante;

    @Column(name = "comision", precision = 14, scale = 2)
    private BigDecimal comision;

    @Column(name = "mensaje_codigo", length = 10)
    private String mensajeCodigo;

    @Column(name = "mensaje_texto", length = 255)
    private String mensajeTexto;

    @Builder.Default
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Builder.Default
    @Column(name = "intentos_confirmacion", nullable = false)
    private Integer intentosConfirmacion = 0;
}
