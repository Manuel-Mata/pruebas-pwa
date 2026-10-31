package com.proyecto.servicios.service.transacciones.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.entity.mongo.CatalogoSnapshot;
import com.proyecto.servicios.entity.mongo.ProductoCatalogo;
import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.exception.ComunicacionGestoPagoException;
import com.proyecto.servicios.exception.EncriptacionGestoPagoException;
import com.proyecto.servicios.exception.TimeoutGestoPagoException;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxResponse;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionSolicitudRequest;
import com.proyecto.servicios.repositorys.mongo.CatalogoSnapshotRepository;
import com.proyecto.servicios.security.gestopago.GestoPagoEncryptionService;
import com.proyecto.servicios.service.GestoPagoAuthorizationHeaderProvider;
import com.proyecto.servicios.service.transacciones.TransaccionPersistenciaService;
import com.proyecto.servicios.service.transacciones.TransaccionService;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransaccionServiceImpl implements TransaccionService {

    // Confirmado en la documentación: tipoFront 1 = recarga telefónica (payload con "telefono").
    // Para el resto de valores se usa "referencia" + "montoPago" (pago con referencia), como
    // documenta GestoPago; falta validar con más ejemplos si existen otros casos.
    private static final Integer TIPO_FRONT_TELEFONO = 1;
    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CatalogoSnapshotRepository catalogoSnapshotRepository;
    private final TransaccionPersistenciaService persistenciaService;
    private final GestoPagoTransaccionClient transaccionClient;
    private final GestoPagoEncryptionService encryptionService;
    private final GestoPagoAuthorizationHeaderProvider authorizationHeaderProvider;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.transaccion.unidad}")
    private String unidad;

    @Override
    public TransaccionResponse procesar(TransaccionSolicitudRequest request, String correoAutenticado) {
        log.info("Inicia procesamiento de transacción (cuentaId={}, idServicio={}, idProducto={}, upc={})",
                request.getCuentaId(), request.getIdServicio(), request.getIdProducto(), request.getIdempotencyKey());

        ProductoCatalogo producto = buscarProductoEnCatalogo(request.getIdServicio(), request.getIdProducto());
        String horaLocal = LocalDateTime.now().format(FORMATO_HORA_LOCAL);

        // El payload se arma ANTES de registrar el intento para poder guardar su JSON tal
        // cual (payloadOriginal) y poder reenviarlo idéntico si hace falta reconciliar.
        Map<String, Object> payload = construirPayload(producto, request, request.getIdempotencyKey(), horaLocal);
        String payloadJson = serializarPayload(payload);

        // Transacción corta de BD: valida cuenta/saldo/duplicado y deja el intento registrado
        // en PENDIENTE antes de llamar a GestoPago (nunca se abre una transacción de BD
        // mientras se espera la red).
        Transaccion transaccion = persistenciaService.registrarIntento(
                request.getCuentaId(), request.getIdServicio(), request.getIdProducto(),
                request.getReferenciaPago(), producto.getPrecio(), request.getIdempotencyKey(), payloadJson,
                correoAutenticado);

        String signed = encryptionService.encriptar(payloadJson);
        String random = UUID.randomUUID().toString();
        String authorization = authorizationHeaderProvider.obtenerHeader(idDistribuidor, codigoDispositivo);

        SendTxResponse respuesta = enviarAGestoPago(authorization, random, signed, transaccion.getId());

        Transaccion actualizada = respuesta.fueExitosa()
                ? persistenciaService.confirmarExitosa(transaccion.getId(), respuesta)
                : persistenciaService.marcarFallida(transaccion.getId(), respuesta, null);

        log.info("Termina procesamiento de transacción (transaccionId={}, upc={}, estado={})",
                actualizada.getId(), actualizada.getUpc(), actualizada.getEstado());

        return mapearRespuesta(actualizada);
    }

    private ProductoCatalogo buscarProductoEnCatalogo(Integer idServicio, Integer idProducto) {
        CatalogoSnapshot snapshot = catalogoSnapshotRepository.findFirstByOrderByFechaSincronizacionDesc()
                .orElseThrow(() -> new CatalogoNoEncontradoException(
                        "Aún no se ha sincronizado el catálogo. Ejecuta primero POST /catalogos/sincronizar."));

        return snapshot.getProductos().stream()
                .filter(p -> Objects.equals(p.getIdServicio(), idServicio) && Objects.equals(p.getIdProducto(), idProducto))
                .findFirst()
                .orElseThrow(() -> new CatalogoNoEncontradoException(
                        "El producto idServicio=" + idServicio + ", idProducto=" + idProducto
                                + " no existe en el catálogo sincronizado."));
    }

    private Map<String, Object> construirPayload(ProductoCatalogo producto, TransaccionSolicitudRequest request,
                                                   String upc, String horaLocal) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idProducto", request.getIdProducto());
        payload.put("idServicio", request.getIdServicio());
        payload.put("horaLocal", horaLocal);
        payload.put("upc", upc);
        payload.put("unidad", unidad);

        if (TIPO_FRONT_TELEFONO.equals(producto.getTipoFront())) {
            payload.put("telefono", request.getReferenciaPago());
        } else {
            payload.put("referencia", request.getReferenciaPago());
            payload.put("montoPago", producto.getPrecio());
        }
        return payload;
    }

    private String serializarPayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new EncriptacionGestoPagoException("No fue posible serializar el payload de la transacción.", e);
        }
    }

    private SendTxResponse enviarAGestoPago(String authorization, String random, String signed, Long transaccionId) {
        try {
            return transaccionClient.enviarTransaccion(authorization, random, signed);
        } catch (RetryableException e) {
            log.warn("Timeout esperando respuesta de sendTx (transaccionId={}); queda en reconciliación.", transaccionId);
            persistenciaService.marcarEnReconciliacion(transaccionId);
            throw new TimeoutGestoPagoException(
                    "GestoPago no respondió a tiempo; la transacción quedó pendiente de reconciliación (confirmTx.do).", e);
        } catch (FeignException e) {
            log.error("Error de comunicación al enviar la transacción a GestoPago (transaccionId={}): {}",
                    transaccionId, e.getMessage());
            persistenciaService.marcarFallida(transaccionId, null, "Error de comunicación con GestoPago: " + e.getMessage());
            throw new ComunicacionGestoPagoException("No fue posible comunicarse con GestoPago", e);
        }
    }

    private TransaccionResponse mapearRespuesta(Transaccion t) {
        return new TransaccionResponse(
                t.getId(), t.getUpc(), t.getEstado(), t.getMonto(),
                t.getIdTxGestoPago(), t.getNumAutorizacion(), t.getMensajeTexto(), t.getFechaSolicitud());
    }
}
