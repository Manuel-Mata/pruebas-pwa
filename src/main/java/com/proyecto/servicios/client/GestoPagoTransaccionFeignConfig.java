package com.proyecto.servicios.client;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Configuración exclusiva del cliente de transacciones (sendTx).
 * Sin @Configuration a propósito: no se aplica a los demás clientes Feign.
 *
 * El read-timeout se deja ligeramente por encima de los 62s que documenta
 * GestoPago como límite antes de que la reconciliación (confirmTx.do) sea
 * obligatoria, para distinguir "no hubo respuesta todavía" de "no va a haber
 * respuesta" antes de marcar la transacción como EN_RECONCILIACION.
 */
public class GestoPagoTransaccionFeignConfig {

    @Bean
    public Request.Options transaccionRequestOptions(
            @Value("${gestopago.transaccion.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${gestopago.transaccion.read-timeout-ms}") long readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS,
                readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public ErrorDecoder transaccionErrorDecoder() {
        return new GestoPagoTransaccionErrorDecoder();
    }

    /**
     * Sin reintentos automáticos: una transacción de pago nunca debe reenviarse
     * sola ante una falla de red (riesgo de doble cobro); la reconciliación con
     * confirmTx.do se maneja explícitamente en el servicio, no aquí.
     */
    @Bean
    public Retryer transaccionRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
