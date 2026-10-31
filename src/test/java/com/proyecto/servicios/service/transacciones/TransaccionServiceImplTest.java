package com.proyecto.servicios.service.transacciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.entity.mongo.CatalogoSnapshot;
import com.proyecto.servicios.entity.mongo.ProductoCatalogo;
import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.exception.ComunicacionGestoPagoException;
import com.proyecto.servicios.exception.TimeoutGestoPagoException;
import com.proyecto.servicios.model.gestopago.MensajeDto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxResponse;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionSolicitudRequest;
import com.proyecto.servicios.repositorys.mongo.CatalogoSnapshotRepository;
import com.proyecto.servicios.security.gestopago.GestoPagoEncryptionService;
import com.proyecto.servicios.service.GestoPagoAuthorizationHeaderProvider;
import com.proyecto.servicios.service.transacciones.Impl.TransaccionServiceImpl;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionServiceImplTest {

    @Mock
    private CatalogoSnapshotRepository catalogoSnapshotRepository;
    @Mock
    private TransaccionPersistenciaService persistenciaService;
    @Mock
    private GestoPagoTransaccionClient transaccionClient;
    @Mock
    private GestoPagoEncryptionService encryptionService;
    @Mock
    private GestoPagoAuthorizationHeaderProvider authorizationHeaderProvider;

    private TransaccionServiceImpl service;

    private static final Integer ID_DISTRIBUIDOR = 83;
    private static final String CODIGO_DISPOSITIVO = "GPS83-TPV-17";
    private static final Long CUENTA_ID = 10L;
    private static final String CORREO_AUTENTICADO = "ana.roma@test.com";

    @BeforeEach
    void setUp() {
        // ObjectMapper real: queremos la serialización real del payload para poder
        // inspeccionar el JSON que se manda a encriptar.
        service = new TransaccionServiceImpl(catalogoSnapshotRepository, persistenciaService,
                transaccionClient, encryptionService, authorizationHeaderProvider, new ObjectMapper());
        ReflectionTestUtils.setField(service, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(service, "codigoDispositivo", CODIGO_DISPOSITIVO);
        ReflectionTestUtils.setField(service, "unidad", "083");
    }

    private TransaccionSolicitudRequest request() {
        TransaccionSolicitudRequest r = new TransaccionSolicitudRequest();
        r.setCuentaId(CUENTA_ID);
        r.setIdServicio(133);
        r.setIdProducto(582);
        r.setReferenciaPago("5555555551");
        r.setIdempotencyKey("upc-1");
        return r;
    }

    private void mockearCatalogoConProducto(Integer tipoFront) {
        ProductoCatalogo producto = new ProductoCatalogo();
        producto.setIdServicio(133);
        producto.setIdProducto(582);
        producto.setTipoFront(tipoFront);
        producto.setPrecio(new BigDecimal("50.00"));

        CatalogoSnapshot snapshot = new CatalogoSnapshot();
        snapshot.setProductos(List.of(producto));

        when(catalogoSnapshotRepository.findFirstByOrderByFechaSincronizacionDesc())
                .thenReturn(Optional.of(snapshot));
    }

    private Transaccion transaccionPendiente() {
        return Transaccion.builder().id(1L).upc("upc-1").estado(EstadoTransaccion.PENDIENTE).build();
    }

    @Test
    void procesarFallaSiElProductoNoExisteEnElCatalogo() {
        when(catalogoSnapshotRepository.findFirstByOrderByFechaSincronizacionDesc()).thenReturn(Optional.empty());

        assertThrows(CatalogoNoEncontradoException.class, () -> service.procesar(request(), CORREO_AUTENTICADO));

        verify(persistenciaService, never()).registrarIntento(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesarArmaPayloadConTelefonoCuandoTipoFrontEsUno() {
        mockearCatalogoConProducto(1);
        when(encryptionService.encriptar(anyString())).thenReturn("signed-payload");
        when(persistenciaService.registrarIntento(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transaccionPendiente());

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("01");
        SendTxResponse respuesta = new SendTxResponse();
        respuesta.setIdTx("123");
        respuesta.setMensaje(mensaje);
        when(transaccionClient.enviarTransaccion(anyString(), anyString(), anyString())).thenReturn(respuesta);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn("Bearer token");
        when(persistenciaService.confirmarExitosa(eq(1L), any())).thenReturn(transaccionPendiente());

        service.procesar(request(), CORREO_AUTENTICADO);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(encryptionService).encriptar(payloadCaptor.capture());
        assertTrue(payloadCaptor.getValue().contains("\"telefono\":\"5555555551\""));
    }

    @Test
    void procesarArmaPayloadConReferenciaYMontoPagoCuandoTipoFrontNoEsUno() {
        mockearCatalogoConProducto(2);
        when(encryptionService.encriptar(anyString())).thenReturn("signed-payload");
        when(persistenciaService.registrarIntento(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transaccionPendiente());

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("01");
        SendTxResponse respuesta = new SendTxResponse();
        respuesta.setIdTx("123");
        respuesta.setMensaje(mensaje);
        when(transaccionClient.enviarTransaccion(anyString(), anyString(), anyString())).thenReturn(respuesta);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn("Bearer token");
        when(persistenciaService.confirmarExitosa(eq(1L), any())).thenReturn(transaccionPendiente());

        service.procesar(request(), CORREO_AUTENTICADO);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(encryptionService).encriptar(payloadCaptor.capture());
        String json = payloadCaptor.getValue();
        assertTrue(json.contains("\"referencia\":\"5555555551\""));
        assertTrue(json.contains("\"montoPago\":50.00") || json.contains("\"montoPago\":50.0"));
    }

    @Test
    void procesarConfirmaExitosaCuandoGestoPagoResponde01() {
        mockearCatalogoConProducto(1);
        when(encryptionService.encriptar(anyString())).thenReturn("signed-payload");
        when(persistenciaService.registrarIntento(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transaccionPendiente());

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("01");
        SendTxResponse respuesta = new SendTxResponse();
        respuesta.setIdTx("123");
        respuesta.setMensaje(mensaje);
        when(transaccionClient.enviarTransaccion(anyString(), anyString(), anyString())).thenReturn(respuesta);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn("Bearer token");

        Transaccion exitosa = Transaccion.builder().id(1L).upc("upc-1").estado(EstadoTransaccion.EXITOSA).build();
        when(persistenciaService.confirmarExitosa(1L, respuesta)).thenReturn(exitosa);

        TransaccionResponse resultado = service.procesar(request(), CORREO_AUTENTICADO);

        assertEquals(EstadoTransaccion.EXITOSA, resultado.getEstado());
        verify(persistenciaService, never()).marcarFallida(any(), any(), any());
    }

    @Test
    void procesarMarcaFallidaCuandoGestoPagoNoResponde01() {
        mockearCatalogoConProducto(1);
        when(encryptionService.encriptar(anyString())).thenReturn("signed-payload");
        when(persistenciaService.registrarIntento(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transaccionPendiente());

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("21");
        mensaje.setTexto("Saldo insuficiente");
        SendTxResponse respuesta = new SendTxResponse();
        respuesta.setIdTx("-1");
        respuesta.setMensaje(mensaje);
        when(transaccionClient.enviarTransaccion(anyString(), anyString(), anyString())).thenReturn(respuesta);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn("Bearer token");

        Transaccion fallida = Transaccion.builder().id(1L).upc("upc-1").estado(EstadoTransaccion.FALLIDA).build();
        when(persistenciaService.marcarFallida(1L, respuesta, null)).thenReturn(fallida);

        TransaccionResponse resultado = service.procesar(request(), CORREO_AUTENTICADO);

        assertEquals(EstadoTransaccion.FALLIDA, resultado.getEstado());
        verify(persistenciaService, never()).confirmarExitosa(any(), any());
    }

    @Test
    void procesarMarcaEnReconciliacionYLanzaTimeoutSiHayRetryableException() {
        mockearCatalogoConProducto(1);
        when(encryptionService.encriptar(anyString())).thenReturn("signed-payload");
        when(persistenciaService.registrarIntento(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transaccionPendiente());
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn("Bearer token");
        when(transaccionClient.enviarTransaccion(anyString(), anyString(), anyString()))
                .thenThrow(mock(RetryableException.class));

        assertThrows(TimeoutGestoPagoException.class, () -> service.procesar(request(), CORREO_AUTENTICADO));

        verify(persistenciaService).marcarEnReconciliacion(1L);
    }

    @Test
    void procesarMarcaFallidaYLanzaComunicacionSiHayFeignException() {
        mockearCatalogoConProducto(1);
        when(encryptionService.encriptar(anyString())).thenReturn("signed-payload");
        when(persistenciaService.registrarIntento(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transaccionPendiente());
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn("Bearer token");
        when(transaccionClient.enviarTransaccion(anyString(), anyString(), anyString()))
                .thenThrow(mock(FeignException.class));

        assertThrows(ComunicacionGestoPagoException.class, () -> service.procesar(request(), CORREO_AUTENTICADO));

        verify(persistenciaService).marcarFallida(eq(1L), eq(null), anyString());
    }
}
