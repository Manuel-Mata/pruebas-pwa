package com.proyecto.servicios.service.transacciones;

import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.gestopago.MensajeDto;
import com.proyecto.servicios.model.gestopago.transaccion.ConfirmTxResponse;
import com.proyecto.servicios.repositorys.transacciones.TransaccionRepository;
import com.proyecto.servicios.security.gestopago.GestoPagoEncryptionService;
import com.proyecto.servicios.service.GestoPagoAuthorizationHeaderProvider;
import com.proyecto.servicios.service.transacciones.Impl.ReconciliacionTransaccionServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliacionTransaccionServiceImplTest {

    @Mock
    private TransaccionRepository transaccionRepository;
    @Mock
    private TransaccionPersistenciaService persistenciaService;
    @Mock
    private GestoPagoTransaccionClient transaccionClient;
    @Mock
    private GestoPagoEncryptionService encryptionService;
    @Mock
    private GestoPagoAuthorizationHeaderProvider authorizationHeaderProvider;

    private ReconciliacionTransaccionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReconciliacionTransaccionServiceImpl(transaccionRepository, persistenciaService,
                transaccionClient, encryptionService, authorizationHeaderProvider);
        ReflectionTestUtils.setField(service, "idDistribuidor", 83);
        ReflectionTestUtils.setField(service, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(service, "maxIntentos", 5);
    }

    private Transaccion pendiente(int intentos) {
        return Transaccion.builder().id(1L).upc("upc-1")
                .estado(EstadoTransaccion.EN_RECONCILIACION)
                .payloadOriginal("{}").intentosConfirmacion(intentos).build();
    }

    @Test
    void noHaceNadaCuandoNoHayTransaccionesPendientes() {
        when(transaccionRepository.findByEstado(EstadoTransaccion.EN_RECONCILIACION)).thenReturn(List.of());

        service.reconciliarPendientes();

        verify(transaccionClient, never()).confirmarTransaccion(anyString(), anyString(), anyString());
    }

    @Test
    void confirmaExitosaCuandoGestoPagoResponde01() {
        when(transaccionRepository.findByEstado(EstadoTransaccion.EN_RECONCILIACION)).thenReturn(List.of(pendiente(1)));
        when(encryptionService.encriptar(anyString())).thenReturn("signed");
        when(authorizationHeaderProvider.obtenerHeader(83, "GPS83-TPV-17")).thenReturn("Bearer token");

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("01");
        ConfirmTxResponse respuesta = new ConfirmTxResponse();
        respuesta.setNumAutorizacion("999");
        respuesta.setMensaje(mensaje);
        when(transaccionClient.confirmarTransaccion(anyString(), anyString(), anyString())).thenReturn(respuesta);

        service.reconciliarPendientes();

        verify(persistenciaService).confirmarExitosaPorReconciliacion(1L, respuesta);
        verify(persistenciaService, never()).marcarFallidaPorReconciliacion(any(), any());
    }

    @Test
    void marcaFallidaCuandoGestoPagoConfirmaQueNoSeAplico() {
        when(transaccionRepository.findByEstado(EstadoTransaccion.EN_RECONCILIACION)).thenReturn(List.of(pendiente(1)));
        when(encryptionService.encriptar(anyString())).thenReturn("signed");
        when(authorizationHeaderProvider.obtenerHeader(83, "GPS83-TPV-17")).thenReturn("Bearer token");

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("41");
        ConfirmTxResponse respuesta = new ConfirmTxResponse();
        respuesta.setNumAutorizacion("-1");
        respuesta.setMensaje(mensaje);
        when(transaccionClient.confirmarTransaccion(anyString(), anyString(), anyString())).thenReturn(respuesta);

        service.reconciliarPendientes();

        verify(persistenciaService).marcarFallidaPorReconciliacion(1L, respuesta);
    }

    @Test
    void registraIntentoFallidoCuandoConfirmTxNoResponde() {
        when(transaccionRepository.findByEstado(EstadoTransaccion.EN_RECONCILIACION)).thenReturn(List.of(pendiente(1)));
        when(encryptionService.encriptar(anyString())).thenReturn("signed");
        when(authorizationHeaderProvider.obtenerHeader(83, "GPS83-TPV-17")).thenReturn("Bearer token");
        when(transaccionClient.confirmarTransaccion(anyString(), anyString(), anyString()))
                .thenThrow(mock(FeignException.class));

        service.reconciliarPendientes();

        verify(persistenciaService).registrarIntentoReconciliacionFallido(1L);
    }

    @Test
    void marcaFallidaDirectamenteCuandoSeAgotanLosIntentos() {
        when(transaccionRepository.findByEstado(EstadoTransaccion.EN_RECONCILIACION)).thenReturn(List.of(pendiente(5)));

        service.reconciliarPendientes();

        verify(transaccionClient, never()).confirmarTransaccion(anyString(), anyString(), anyString());
        verify(persistenciaService).marcarFallida(eq(1L), eq(null), anyString());
    }
}
