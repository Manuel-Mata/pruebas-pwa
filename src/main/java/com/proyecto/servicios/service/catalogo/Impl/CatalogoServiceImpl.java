package com.proyecto.servicios.service.catalogo.Impl;

import com.proyecto.servicios.client.GestoPagoCatalogoClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatalogoSnapshot;
import com.proyecto.servicios.entity.mongo.ProductoCatalogo;
import com.proyecto.servicios.exception.AutenticacionGestoPagoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.exception.ComunicacionGestoPagoException;
import com.proyecto.servicios.exception.LimiteDiarioSincronizacionException;
import com.proyecto.servicios.exception.RespuestaNoExitosaGestoPagoException;
import com.proyecto.servicios.exception.TimeoutGestoPagoException;
import com.proyecto.servicios.mapper.CatalogoMapper;
import com.proyecto.servicios.model.catalogo.CatalogoSincronizacionResponse;
import com.proyecto.servicios.model.catalogo.ProductoCatalogoResponse;
import com.proyecto.servicios.model.gestopago.catalogo.CatalogoResponse;
import com.proyecto.servicios.repositorys.mongo.CatalogoSnapshotRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.catalogo.CatalogoService;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private static final String CODIGO_EXITO = "01";

    private final GestoPagoCatalogoClient catalogoClient;
    private final GestoPagoTokenService tokenService;
    private final CatalogoSnapshotRepository snapshotRepository;
    private final CatalogoMapper catalogoMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.catalogo.max-sincronizaciones-diarias}")
    private int maxSincronizacionesDiarias;

    @Override
    public CatalogoSincronizacionResponse sincronizar() {
        log.info("Inicia sincronización del catálogo GestoPago (distribuidor={})", idDistribuidor);

        verificarLimiteDiario();
        String authorization = construirAuthorizationHeader();

        CatalogoResponse respuesta = consultarCatalogoRemoto(authorization);

        String codigo = respuesta.getMensaje() != null ? respuesta.getMensaje().getCodigo() : null;
        String texto = respuesta.getMensaje() != null ? respuesta.getMensaje().getTexto() : null;

        if (!CODIGO_EXITO.equals(codigo)) {
            log.error("GestoPago respondió con error al consultar el catálogo: {} - {}", codigo, texto);
            throw new RespuestaNoExitosaGestoPagoException(
                    "GestoPago rechazó la consulta del catálogo (código " + codigo + "): " + texto, null);
        }

        List<ProductoCatalogo> productos = catalogoMapper.toProductos(respuesta.getProductos());

        CatalogoSnapshot snapshot = new CatalogoSnapshot();
        snapshot.setFechaSincronizacion(Instant.now());
        snapshot.setCodigoRespuesta(codigo);
        snapshot.setMensajeRespuesta(texto);
        snapshot.setTotalProductos(productos.size());
        snapshot.setProductos(productos);

        snapshotRepository.save(snapshot);

        log.info("Termina sincronización del catálogo GestoPago: {} producto(s) guardados", productos.size());

        return new CatalogoSincronizacionResponse(
                snapshot.getFechaSincronizacion(),
                snapshot.getCodigoRespuesta(),
                snapshot.getMensajeRespuesta(),
                snapshot.getTotalProductos());
    }

    @Override
    public List<ProductoCatalogoResponse> obtenerProductos(Integer idServicio) {
        CatalogoSnapshot snapshot = snapshotRepository.findFirstByOrderByFechaSincronizacionDesc()
                .orElseThrow(() -> new CatalogoNoEncontradoException(
                        "Aún no se ha sincronizado el catálogo. Ejecuta primero POST /catalogos/sincronizar."));

        List<ProductoCatalogo> productos = snapshot.getProductos();
        if (idServicio != null) {
            productos = productos.stream()
                    .filter(p -> Objects.equals(p.getIdServicio(), idServicio))
                    .collect(Collectors.toList());
        }

        return catalogoMapper.toResponses(productos);
    }

    private void verificarLimiteDiario() {
        Instant inicioDelDia = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long sincronizacionesHoy = snapshotRepository.countByFechaSincronizacionBetween(inicioDelDia, Instant.now());

        if (sincronizacionesHoy >= maxSincronizacionesDiarias) {
            log.warn("Límite diario de sincronizaciones del catálogo alcanzado ({}/{})",
                    sincronizacionesHoy, maxSincronizacionesDiarias);
            throw new LimiteDiarioSincronizacionException(maxSincronizacionesDiarias);
        }
    }

    private String construirAuthorizationHeader() {
        Optional<GestoPagoToken> token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);

        GestoPagoToken activo = token.orElseThrow(() -> new AutenticacionGestoPagoException(
                "No hay un token vigente de GestoPago para distribuidor=" + idDistribuidor
                        + ". Aún no se ha ejecutado la renovación automática."));

        String tipo = activo.getTokenType() != null ? activo.getTokenType() : "Bearer";
        return tipo + " " + activo.getToken();
    }

    private CatalogoResponse consultarCatalogoRemoto(String authorization) {
        try {
            return catalogoClient.obtenerCatalogo(authorization);
        } catch (RetryableException e) {
            log.error("Timeout al consultar el catálogo de GestoPago: {}", e.getMessage());
            throw new TimeoutGestoPagoException("GestoPago no respondió a tiempo al consultar el catálogo", e);
        } catch (FeignException e) {
            log.error("Error de comunicación al consultar el catálogo de GestoPago: {}", e.getMessage());
            throw new ComunicacionGestoPagoException("No fue posible comunicarse con GestoPago", e);
        }
    }
}
