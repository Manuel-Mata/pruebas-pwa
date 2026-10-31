package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.AutenticacionGestoPagoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Arma el header Authorization a partir del token vigente de GestoPago.
 * Compartido por todos los clientes GestoPago (catálogo, transacciones, etc.)
 * para no repetir esta lógica en cada servicio.
 */
@Component
@RequiredArgsConstructor
public class GestoPagoAuthorizationHeaderProvider {

    private final GestoPagoTokenService tokenService;

    public String obtenerHeader(Integer idDistribuidor, String codigoDispositivo) {
        GestoPagoToken activo = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .orElseThrow(() -> new AutenticacionGestoPagoException(
                        "No hay un token vigente de GestoPago para distribuidor=" + idDistribuidor
                                + ". Aún no se ha ejecutado la renovación automática."));

        String tipo = activo.getTokenType() != null ? activo.getTokenType() : "Bearer";
        return tipo + " " + activo.getToken();
    }
}
