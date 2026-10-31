package com.proyecto.servicios.client;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Configuración exclusiva del cliente de catálogo.
 * No lleva @Configuration a propósito: así no se aplica a los demás clientes Feign.
 */
public class GestoPagoCatalogoFeignConfig {

    @Bean
    public Request.Options catalogoRequestOptions(
            @Value("${gestopago.catalogo.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${gestopago.catalogo.read-timeout-ms}") long readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS,
                readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public ErrorDecoder catalogoErrorDecoder() {
        return new GestoPagoCatalogoErrorDecoder();
    }

    /** Sin reintentos: la API bloquea la IP si se llama más de 3 veces al día. */
    @Bean
    public Retryer catalogoRetryer() {
        return Retryer.NEVER_RETRY;
    }
}