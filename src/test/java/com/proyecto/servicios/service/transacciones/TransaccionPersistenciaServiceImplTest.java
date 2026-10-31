package com.proyecto.servicios.service.transacciones;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.model.gestopago.MensajeDto;
import com.proyecto.servicios.model.gestopago.transaccion.ConfirmTxResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionRepository;
import com.proyecto.servicios.service.transacciones.Impl.TransaccionPersistenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionPersistenciaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private TransaccionRepository transaccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private TransaccionPersistenciaServiceImpl service;

    private static final Long CUENTA_ID = 10L;
    private static final Long CLIENTE_ID = 20L;
    private static final Long OTRO_CLIENTE_ID = 99L;
    private static final String CORREO_AUTENTICADO = "ana.roma@test.com";

    @BeforeEach
    void setUp() {
        service = new TransaccionPersistenciaServiceImpl(cuentaRepository, transaccionRepository, usuarioRepository);
    }

    private Cliente cliente(Long id) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        return cliente;
    }

    private Cuenta cuentaActiva(BigDecimal saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(CUENTA_ID);
        cuenta.setSaldo(saldo);
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        cuenta.setCliente(cliente(CLIENTE_ID));
        return cuenta;
    }

    private void mockUsuarioAutenticadoValido() {
        Usuario usuario = new Usuario();
        usuario.setCorreo(CORREO_AUTENTICADO);
        usuario.setCliente(cliente(CLIENTE_ID));
        lenient().when(usuarioRepository.findByCorreo(CORREO_AUTENTICADO)).thenReturn(Optional.of(usuario));
    }

    @Test
    void registrarIntentoGuardaTransaccionPendienteCuandoTodoEsValido() {
        mockUsuarioAutenticadoValido();
        when(transaccionRepository.findByUpc("upc-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.of(cuentaActiva(new BigDecimal("100.00"))));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaccion resultado = service.registrarIntento(CUENTA_ID, 133, 582, "5555555551",
                new BigDecimal("50.00"), "upc-1", "{\"upc\":\"upc-1\"}", CORREO_AUTENTICADO);

        assertEquals(EstadoTransaccion.PENDIENTE, resultado.getEstado());
        assertEquals("upc-1", resultado.getUpc());
        verify(transaccionRepository).save(any(Transaccion.class));
    }

    @Test
    void registrarIntentoFallaSiElUpcYaExiste() {
        when(transaccionRepository.findByUpc("upc-1")).thenReturn(Optional.of(new Transaccion()));

        assertThrows(TransaccionDuplicadaException.class, () -> service.registrarIntento(
                CUENTA_ID, 133, 582, "5555555551", new BigDecimal("50.00"), "upc-1", "{}", CORREO_AUTENTICADO));

        verify(cuentaRepository, never()).buscarConLockParaActualizar(anyLong());
    }

    @Test
    void registrarIntentoFallaSiLaCuentaNoExiste() {
        when(transaccionRepository.findByUpc("upc-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> service.registrarIntento(
                CUENTA_ID, 133, 582, "5555555551", new BigDecimal("50.00"), "upc-1", "{}", CORREO_AUTENTICADO));
    }

    @Test
    void registrarIntentoFallaSiLaCuentaNoPerteneceAlClienteAutenticado() {
        Cuenta cuentaDeOtroCliente = cuentaActiva(new BigDecimal("100.00"));
        cuentaDeOtroCliente.setCliente(cliente(OTRO_CLIENTE_ID));

        mockUsuarioAutenticadoValido();
        when(transaccionRepository.findByUpc("upc-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.of(cuentaDeOtroCliente));

        assertThrows(OperacionNoAutorizadaException.class, () -> service.registrarIntento(
                CUENTA_ID, 133, 582, "5555555551", new BigDecimal("50.00"), "upc-1", "{}", CORREO_AUTENTICADO));

        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void registrarIntentoFallaSiElUsuarioAutenticadoNoExiste() {
        when(transaccionRepository.findByUpc("upc-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.of(cuentaActiva(new BigDecimal("100.00"))));
        when(usuarioRepository.findByCorreo(CORREO_AUTENTICADO)).thenReturn(Optional.empty());

        assertThrows(OperacionNoAutorizadaException.class, () -> service.registrarIntento(
                CUENTA_ID, 133, 582, "5555555551", new BigDecimal("50.00"), "upc-1", "{}", CORREO_AUTENTICADO));
    }

    @Test
    void registrarIntentoFallaSiLaCuentaEstaInactiva() {
        Cuenta cuenta = cuentaActiva(new BigDecimal("100.00"));
        cuenta.setEstatus(EstatusCuenta.INACTIVA);

        mockUsuarioAutenticadoValido();
        when(transaccionRepository.findByUpc("upc-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        assertThrows(CuentaInactivaException.class, () -> service.registrarIntento(
                CUENTA_ID, 133, 582, "5555555551", new BigDecimal("50.00"), "upc-1", "{}", CORREO_AUTENTICADO));
    }

    @Test
    void registrarIntentoFallaSiElSaldoEsInsuficiente() {
        mockUsuarioAutenticadoValido();
        when(transaccionRepository.findByUpc("upc-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.of(cuentaActiva(new BigDecimal("10.00"))));

        assertThrows(SaldoInsuficienteException.class, () -> service.registrarIntento(
                CUENTA_ID, 133, 582, "5555555551", new BigDecimal("50.00"), "upc-1", "{}", CORREO_AUTENTICADO));
    }

    @Test
    void confirmarExitosaDebitaElSaldoDeLaCuenta() {
        Cuenta cuenta = cuentaActiva(new BigDecimal("100.00"));
        Transaccion transaccion = Transaccion.builder()
                .id(1L).cuenta(cuenta).upc("upc-1").monto(new BigDecimal("30.00"))
                .estado(EstadoTransaccion.PENDIENTE).build();

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        SendTxResponse respuesta = new SendTxResponse();
        respuesta.setIdTx("123");
        respuesta.setMensaje(mensaje);

        when(transaccionRepository.findById(1L)).thenReturn(Optional.of(transaccion));
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaccion resultado = service.confirmarExitosa(1L, respuesta);

        assertEquals(EstadoTransaccion.EXITOSA, resultado.getEstado());
        assertEquals(new BigDecimal("70.00"), cuenta.getSaldo());
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    void marcarFallidaNoTocaElSaldoDeLaCuenta() {
        Transaccion transaccion = Transaccion.builder()
                .id(1L).cuenta(cuentaActiva(new BigDecimal("100.00"))).upc("upc-1")
                .monto(new BigDecimal("30.00")).estado(EstadoTransaccion.PENDIENTE).build();

        when(transaccionRepository.findById(1L)).thenReturn(Optional.of(transaccion));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaccion resultado = service.marcarFallida(1L, null, "Error de comunicación");

        assertEquals(EstadoTransaccion.FALLIDA, resultado.getEstado());
        assertEquals("Error de comunicación", resultado.getMensajeTexto());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void marcarEnReconciliacionIncrementaLosIntentos() {
        Transaccion transaccion = Transaccion.builder()
                .id(1L).cuenta(cuentaActiva(new BigDecimal("100.00"))).upc("upc-1")
                .estado(EstadoTransaccion.PENDIENTE).intentosConfirmacion(0).build();

        when(transaccionRepository.findById(1L)).thenReturn(Optional.of(transaccion));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaccion resultado = service.marcarEnReconciliacion(1L);

        assertEquals(EstadoTransaccion.EN_RECONCILIACION, resultado.getEstado());
        assertEquals(1, resultado.getIntentosConfirmacion());
    }

    @Test
    void confirmarExitosaPorReconciliacionDebitaElSaldo() {
        Cuenta cuenta = cuentaActiva(new BigDecimal("100.00"));
        Transaccion transaccion = Transaccion.builder()
                .id(1L).cuenta(cuenta).upc("upc-1").monto(new BigDecimal("20.00"))
                .estado(EstadoTransaccion.EN_RECONCILIACION).build();

        MensajeDto mensaje = new MensajeDto();
        mensaje.setCodigo("01");
        ConfirmTxResponse respuesta = new ConfirmTxResponse();
        respuesta.setNumAutorizacion("999");
        respuesta.setMensaje(mensaje);

        when(transaccionRepository.findById(1L)).thenReturn(Optional.of(transaccion));
        when(cuentaRepository.buscarConLockParaActualizar(CUENTA_ID)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaccion resultado = service.confirmarExitosaPorReconciliacion(1L, respuesta);

        assertEquals(EstadoTransaccion.EXITOSA, resultado.getEstado());
        assertEquals(new BigDecimal("80.00"), cuenta.getSaldo());
    }
}
