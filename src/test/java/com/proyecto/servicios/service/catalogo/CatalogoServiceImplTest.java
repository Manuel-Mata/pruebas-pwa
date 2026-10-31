package com.proyecto.servicios.service.catalogo;

import com.proyecto.servicios.client.GestoPagoCatalogoClient;
import com.proyecto.servicios.entity.mongo.CatalogoSnapshot;
import com.proyecto.servicios.entity.mongo.ProductoCatalogo;
import com.proyecto.servicios.exception.AutenticacionGestoPagoException;
import com.proyecto.servicios.exception.CatalogoNoEncontradoException;
import com.proyecto.servicios.exception.LimiteDiarioSincronizacionException;
import com.proyecto.servicios.exception.RespuestaNoExitosaGestoPagoException;
import com.proyecto.servicios.exception.TimeoutGestoPagoException;
import com.proyecto.servicios.mapper.CatalogoMapper;
import com.proyecto.servicios.model.catalogo.CatalogoSincronizacionResponse;
import com.proyecto.servicios.model.catalogo.ProductoCatalogoResponse;
import com.proyecto.servicios.model.gestopago.catalogo.CatalogoResponse;
import com.proyecto.servicios.model.gestopago.MensajeDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductoDto;
import com.proyecto.servicios.repositorys.mongo.CatalogoSnapshotRepository;
import com.proyecto.servicios.service.GestoPagoAuthorizationHeaderProvider;
import com.proyecto.servicios.service.catalogo.Impl.CatalogoServiceImpl;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    @Mock
    private GestoPagoCatalogoClient catalogoClient;
    @Mock
    private GestoPagoAuthorizationHeaderProvider authorizationHeaderProvider;
    @Mock
    private CatalogoSnapshotRepository snapshotRepository;
    @Mock
    private CatalogoMapper catalogoMapper;

    private CatalogoServiceImpl service;

    private static final Integer ID_DISTRIBUIDOR = 83;
    private static final String CODIGO_DISPOSITIVO = "GPS83-TPV-17";
    private static final int MAX_SINCRONIZACIONES_DIARIAS = 2;

    @BeforeEach
    void setUp() {
        service = new CatalogoServiceImpl(catalogoClient, authorizationHeaderProvider, snapshotRepository, catalogoMapper);
        ReflectionTestUtils.setField(service, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(service, "codigoDispositivo", CODIGO_DISPOSITIVO);
        ReflectionTestUtils.setField(service, "maxSincronizacionesDiarias", MAX_SINCRONIZACIONES_DIARIAS);
    }

    @Test
    void sincronizarGuardaUnNuevoSnapshotCuandoLaRespuestaEsExitosa() {
        when(snapshotRepository.countByFechaSincronizacionBetween(any(), any())).thenReturn(0L);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn("Bearer token-de-prueba");

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");

        ProductoDto productoDto = new ProductoDto();
        productoDto.setIdServicio(79);
        productoDto.setIdProducto(209);

        CatalogoResponse respuestaRemota = new CatalogoResponse();
        respuestaRemota.setMensaje(mensaje);
        respuestaRemota.setProductos(List.of(productoDto));

        when(catalogoClient.obtenerCatalogo(anyString())).thenReturn(respuestaRemota);

        ProductoCatalogo productoMongo = new ProductoCatalogo();
        productoMongo.setIdServicio(79);
        when(catalogoMapper.toProductos(respuestaRemota.getProductos())).thenReturn(List.of(productoMongo));

        CatalogoSincronizacionResponse resultado = service.sincronizar();

        assertEquals("01", resultado.getCodigoRespuesta());
        assertEquals(1, resultado.getTotalProductos());
        verify(snapshotRepository).save(any(CatalogoSnapshot.class));
    }

    @Test
    void sincronizarRechazaCuandoYaSeAlcanzoElLimiteDiario() {
        when(snapshotRepository.countByFechaSincronizacionBetween(any(), any()))
                .thenReturn((long) MAX_SINCRONIZACIONES_DIARIAS);

        assertThrows(LimiteDiarioSincronizacionException.class, () -> service.sincronizar());

        verify(authorizationHeaderProvider, never()).obtenerHeader(any(), any());
        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void sincronizarFallaSiNoHayTokenVigente() {
        when(snapshotRepository.countByFechaSincronizacionBetween(any(), any())).thenReturn(0L);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenThrow(new AutenticacionGestoPagoException("No hay un token vigente de GestoPago para distribuidor=" + ID_DISTRIBUIDOR));

        assertThrows(AutenticacionGestoPagoException.class, () -> service.sincronizar());

        verify(catalogoClient, never()).obtenerCatalogo(anyString());
    }

    @Test
    void sincronizarFallaSiGestoPagoRespondeConCodigoDeError() {
        when(snapshotRepository.countByFechaSincronizacionBetween(any(), any())).thenReturn(0L);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn("Bearer token-de-prueba");

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("99");
        mensaje.setTexto("Error interno");

        CatalogoResponse respuestaRemota = new CatalogoResponse();
        respuestaRemota.setMensaje(mensaje);

        when(catalogoClient.obtenerCatalogo(anyString())).thenReturn(respuestaRemota);

        assertThrows(RespuestaNoExitosaGestoPagoException.class, () -> service.sincronizar());

        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void sincronizarConvierteTimeoutDeFeignEnTimeoutGestoPagoException() {
        when(snapshotRepository.countByFechaSincronizacionBetween(any(), any())).thenReturn(0L);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn("Bearer token-de-prueba");
        when(catalogoClient.obtenerCatalogo(anyString())).thenThrow(mock(RetryableException.class));

        assertThrows(TimeoutGestoPagoException.class, () -> service.sincronizar());
    }

    @Test
    void sincronizarConvierteErrorDeFeignEnComunicacionGestoPagoException() {
        when(snapshotRepository.countByFechaSincronizacionBetween(any(), any())).thenReturn(0L);
        when(authorizationHeaderProvider.obtenerHeader(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn("Bearer token-de-prueba");
        when(catalogoClient.obtenerCatalogo(anyString())).thenThrow(mock(FeignException.class));

        assertThrows(com.proyecto.servicios.exception.ComunicacionGestoPagoException.class,
                () -> service.sincronizar());
    }

    @Test
    void obtenerProductosFallaSiNuncaSeHaSincronizado() {
        when(snapshotRepository.findFirstByOrderByFechaSincronizacionDesc()).thenReturn(Optional.empty());

        assertThrows(CatalogoNoEncontradoException.class, () -> service.obtenerProductos(null));
    }

    @Test
    void obtenerProductosFiltraPorIdServicioCuandoSeIndica() {
        ProductoCatalogo productoBuscado = new ProductoCatalogo();
        productoBuscado.setIdServicio(79);
        ProductoCatalogo otroProducto = new ProductoCatalogo();
        otroProducto.setIdServicio(80);

        CatalogoSnapshot snapshot = new CatalogoSnapshot();
        snapshot.setFechaSincronizacion(Instant.now());
        snapshot.setProductos(List.of(productoBuscado, otroProducto));

        when(snapshotRepository.findFirstByOrderByFechaSincronizacionDesc()).thenReturn(Optional.of(snapshot));

        ProductoCatalogoResponse respuestaEsperada = new ProductoCatalogoResponse();
        respuestaEsperada.setIdServicio(79);
        when(catalogoMapper.toResponses(List.of(productoBuscado))).thenReturn(List.of(respuestaEsperada));

        List<ProductoCatalogoResponse> resultado = service.obtenerProductos(79);

        assertEquals(1, resultado.size());
        assertEquals(79, resultado.get(0).getIdServicio());
    }
}
