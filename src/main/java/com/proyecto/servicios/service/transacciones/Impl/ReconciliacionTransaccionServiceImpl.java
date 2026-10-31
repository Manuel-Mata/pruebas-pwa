package com.proyecto.servicios.service.transacciones.Impl;

import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.gestopago.transaccion.ConfirmTxResponse;
import com.proyecto.servicios.repositorys.transacciones.TransaccionRepository;
import com.proyecto.servicios.security.gestopago.GestoPagoEncryptionService;
import com.proyecto.servicios.service.GestoPagoAuthorizationHeaderProvider;
import com.proyecto.servicios.service.transacciones.ReconciliacionTransaccionService;
import com.proyecto.servicios.service.transacciones.TransaccionPersistenciaService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Reconciliación de transacciones que no tuvieron respuesta a tiempo en sendTx.do
 * (ver GestoPagoTransaccionFeignConfig: sin reintentos automáticos ahí a propósito,
 * precisamente porque este proceso es el que decide qué hacer con ellas).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliacionTransaccionServiceImpl implements ReconciliacionTransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final TransaccionPersistenciaService persistenciaService;
    private final GestoPagoTransaccionClient transaccionClient;
    private final GestoPagoEncryptionService encryptionService;
    private final GestoPagoAuthorizationHeaderProvider authorizationHeaderProvider;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.transaccion.reconciliacion-max-intentos:5}")
    private int maxIntentos;

    @Override
    @Scheduled(fixedRateString = "${gestopago.transaccion.reconciliacion-rate-ms:300000}", initialDelay = 60000)
    public void reconciliarPendientes() {
        List<Transaccion> pendientes = transaccionRepository.findByEstado(EstadoTransaccion.EN_RECONCILIACION);

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("Reconciliación: {} transacción(es) pendiente(s) de confirmar", pendientes.size());
        for (Transaccion transaccion : pendientes) {
            reconciliarUna(transaccion);
        }
    }

    private void reconciliarUna(Transaccion transaccion) {
        if (transaccion.getIntentosConfirmacion() >= maxIntentos) {
            log.error("Transacción {} (upc={}) agotó los {} intentos de reconciliación; se marca FALLIDA manualmente.",
                    transaccion.getId(), transaccion.getUpc(), maxIntentos);
            persistenciaService.marcarFallida(transaccion.getId(), null,
                    "Reconciliación agotada tras " + maxIntentos + " intentos sin respuesta concluyente de GestoPago. "
                            + "Requiere revisión manual (ver UPC en el log).");
            return;
        }

        try {
            String signed = encryptionService.encriptar(transaccion.getPayloadOriginal());
            String random = UUID.randomUUID().toString();
            String authorization = authorizationHeaderProvider.obtenerHeader(idDistribuidor, codigoDispositivo);

            ConfirmTxResponse respuesta = transaccionClient.confirmarTransaccion(authorization, random, signed);

            if (respuesta.fueExitosa()) {
                persistenciaService.confirmarExitosaPorReconciliacion(transaccion.getId(), respuesta);
            } else {
                persistenciaService.marcarFallidaPorReconciliacion(transaccion.getId(), respuesta);
            }
        } catch (FeignException e) {
            log.warn("confirmTx.do no respondió de forma concluyente para la transacción {} (upc={}): {}",
                    transaccion.getId(), transaccion.getUpc(), e.getMessage());
            persistenciaService.registrarIntentoReconciliacionFallido(transaccion.getId());
        } catch (Exception e) {
            log.error("Error inesperado al reconciliar la transacción {} (upc={}): {}",
                    transaccion.getId(), transaccion.getUpc(), e.getMessage(), e);
            persistenciaService.registrarIntentoReconciliacionFallido(transaccion.getId());
        }
    }
}
