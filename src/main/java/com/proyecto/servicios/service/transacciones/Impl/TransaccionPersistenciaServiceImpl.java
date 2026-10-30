package com.proyecto.servicios.service.transacciones.Impl;

import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.model.gestopago.transaccion.ConfirmTxResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxResponse;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionRepository;
import com.proyecto.servicios.service.transacciones.TransaccionPersistenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransaccionPersistenciaServiceImpl implements TransaccionPersistenciaService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    @Override
    @Transactional
    public Transaccion registrarIntento(Long cuentaId, Integer idServicio, Integer idProducto,
                                         String referenciaPago, BigDecimal monto, String upc, String payloadOriginal) {

        if (transaccionRepository.findByUpc(upc).isPresent()) {
            throw new TransaccionDuplicadaException(upc);
        }

        Cuenta cuenta = cuentaRepository.buscarConLockParaActualizar(cuentaId)
                .orElseThrow(CuentaNoEncontradaException::new);

        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new CuentaInactivaException();
        }

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException();
        }

        Transaccion transaccion = Transaccion.builder()
                .cuenta(cuenta)
                .upc(upc)
                .idServicio(idServicio)
                .idProducto(idProducto)
                .referenciaPago(referenciaPago)
                .monto(monto)
                .payloadOriginal(payloadOriginal)
                .estado(EstadoTransaccion.PENDIENTE)
                .build();

        return transaccionRepository.save(transaccion);
    }

    @Override
    @Transactional
    public Transaccion confirmarExitosa(Long transaccionId, SendTxResponse respuesta) {
        Transaccion transaccion = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción " + transaccionId + " no encontrada"));

        aplicarRespuesta(transaccion, respuesta);
        transaccion.setEstado(EstadoTransaccion.EXITOSA);

        Cuenta cuenta = cuentaRepository.buscarConLockParaActualizar(transaccion.getCuenta().getId())
                .orElseThrow(CuentaNoEncontradaException::new);
        cuenta.setSaldo(cuenta.getSaldo().subtract(transaccion.getMonto()));
        cuentaRepository.save(cuenta);

        log.info("Transacción {} (upc={}) exitosa. Saldo restante de la cuenta {}: {}",
                transaccionId, transaccion.getUpc(), cuenta.getId(), cuenta.getSaldo());

        return transaccionRepository.save(transaccion);
    }

    @Override
    @Transactional
    public Transaccion marcarFallida(Long transaccionId, SendTxResponse respuesta, String mensajeFallback) {
        Transaccion transaccion = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción " + transaccionId + " no encontrada"));

        if (respuesta != null) {
            aplicarRespuesta(transaccion, respuesta);
        } else {
            transaccion.setFechaRespuesta(LocalDateTime.now());
            transaccion.setMensajeTexto(mensajeFallback);
        }
        transaccion.setEstado(EstadoTransaccion.FALLIDA);

        log.warn("Transacción {} (upc={}) marcada como FALLIDA: {}",
                transaccionId, transaccion.getUpc(), transaccion.getMensajeTexto());

        return transaccionRepository.save(transaccion);
    }

    @Override
    @Transactional
    public Transaccion marcarEnReconciliacion(Long transaccionId) {
        Transaccion transaccion = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción " + transaccionId + " no encontrada"));

        transaccion.setEstado(EstadoTransaccion.EN_RECONCILIACION);
        transaccion.setIntentosConfirmacion(transaccion.getIntentosConfirmacion() + 1);

        log.warn("Transacción {} (upc={}) queda EN_RECONCILIACION (timeout de sendTx).",
                transaccionId, transaccion.getUpc());

        return transaccionRepository.save(transaccion);
    }

    @Override
    @Transactional
    public Transaccion confirmarExitosaPorReconciliacion(Long transaccionId, ConfirmTxResponse respuesta) {
        Transaccion transaccion = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción " + transaccionId + " no encontrada"));

        aplicarRespuestaConfirm(transaccion, respuesta);
        transaccion.setEstado(EstadoTransaccion.EXITOSA);

        Cuenta cuenta = cuentaRepository.buscarConLockParaActualizar(transaccion.getCuenta().getId())
                .orElseThrow(CuentaNoEncontradaException::new);
        cuenta.setSaldo(cuenta.getSaldo().subtract(transaccion.getMonto()));
        cuentaRepository.save(cuenta);

        log.info("Reconciliación: transacción {} (upc={}) confirmada EXITOSA. Saldo restante de la cuenta {}: {}",
                transaccionId, transaccion.getUpc(), cuenta.getId(), cuenta.getSaldo());

        return transaccionRepository.save(transaccion);
    }

    @Override
    @Transactional
    public Transaccion marcarFallidaPorReconciliacion(Long transaccionId, ConfirmTxResponse respuesta) {
        Transaccion transaccion = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción " + transaccionId + " no encontrada"));

        aplicarRespuestaConfirm(transaccion, respuesta);
        transaccion.setEstado(EstadoTransaccion.FALLIDA);

        log.warn("Reconciliación: transacción {} (upc={}) confirmada FALLIDA: {}",
                transaccionId, transaccion.getUpc(), transaccion.getMensajeTexto());

        return transaccionRepository.save(transaccion);
    }

    @Override
    @Transactional
    public Transaccion registrarIntentoReconciliacionFallido(Long transaccionId) {
        Transaccion transaccion = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción " + transaccionId + " no encontrada"));

        transaccion.setIntentosConfirmacion(transaccion.getIntentosConfirmacion() + 1);

        log.warn("Reconciliación: confirmTx.do no respondió de forma concluyente para la transacción {} (upc={}), intento #{}",
                transaccionId, transaccion.getUpc(), transaccion.getIntentosConfirmacion());

        return transaccionRepository.save(transaccion);
    }

    private void aplicarRespuesta(Transaccion transaccion, SendTxResponse respuesta) {
        transaccion.setFechaRespuesta(LocalDateTime.now());
        transaccion.setIdTxGestoPago(respuesta.getIdTx());
        transaccion.setNumAutorizacion(respuesta.getNumAutorizacion());
        transaccion.setSaldoResultante(respuesta.getSaldo());
        transaccion.setComision(respuesta.getComision());
        if (respuesta.getMensaje() != null) {
            transaccion.setMensajeCodigo(respuesta.getMensaje().getCodigo());
            transaccion.setMensajeTexto(respuesta.getMensaje().getTexto());
        }
    }

    private void aplicarRespuestaConfirm(Transaccion transaccion, ConfirmTxResponse respuesta) {
        transaccion.setFechaRespuesta(LocalDateTime.now());
        transaccion.setNumAutorizacion(respuesta.getNumAutorizacion());
        if (respuesta.getMensaje() != null) {
            transaccion.setMensajeCodigo(respuesta.getMensaje().getCodigo());
            transaccion.setMensajeTexto(respuesta.getMensaje().getTexto());
        }
    }
}
