package com.proyecto.servicios.service.transacciones;

import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.model.gestopago.transaccion.ConfirmTxResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxResponse;

import java.math.BigDecimal;

/**
 * Encapsula las operaciones de persistencia de una transacción en transacciones
 * de base de datos cortas, separadas de la llamada HTTP a GestoPago (que puede
 * tardar hasta el read-timeout configurado). Así nunca se mantiene abierta una
 * transacción/lock de base de datos mientras se espera la red.
 */
public interface TransaccionPersistenciaService {

    /**
     * Valida cuenta (existe, activa, con saldo) y que el upc no esté usado,
     * y guarda la transacción en estado PENDIENTE. Lockea la cuenta brevemente
     * mientras hace estas validaciones para evitar condiciones de carrera.
     */
    Transaccion registrarIntento(Long cuentaId, Integer idServicio, Integer idProducto,
                                  String referenciaPago, BigDecimal monto, String upc, String payloadOriginal);

    /** Marca la transacción como exitosa con los datos de la respuesta y debita el saldo de la cuenta. */
    Transaccion confirmarExitosa(Long transaccionId, SendTxResponse respuesta);

    /** Marca la transacción como fallida; no se toca el saldo de la cuenta. */
    Transaccion marcarFallida(Long transaccionId, SendTxResponse respuesta, String mensajeFallback);

    /** Marca la transacción como pendiente de reconciliación (confirmTx.do) tras un timeout. */
    Transaccion marcarEnReconciliacion(Long transaccionId);

    /** confirmTx.do confirmó que la transacción sí se aplicó: se marca exitosa y se debita el saldo. */
    Transaccion confirmarExitosaPorReconciliacion(Long transaccionId, ConfirmTxResponse respuesta);

    /** confirmTx.do confirmó que la transacción NO se aplicó: se marca fallida, no se toca el saldo. */
    Transaccion marcarFallidaPorReconciliacion(Long transaccionId, ConfirmTxResponse respuesta);

    /** confirmTx.do tampoco respondió de forma concluyente: se deja en reconciliación para el siguiente intento. */
    Transaccion registrarIntentoReconciliacionFallido(Long transaccionId);
}
