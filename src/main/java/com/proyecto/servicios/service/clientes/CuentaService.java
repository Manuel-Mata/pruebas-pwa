package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.onboarding.ClienteInactivoException;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.onboarding.ErrorValidacionException;
import com.proyecto.servicios.model.CuentaCreacionRequest;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.SaldoResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;

    @Value("${onboarding.cuenta.saldo-inicial:0.00}")
    private BigDecimal saldoInicialPorDefecto;

    @Transactional(readOnly = true)
    public CuentaResponse obtener(String numeroCuenta) {
        return aRespuesta(obtenerEntidad(numeroCuenta));
    }

    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        Cuenta cuenta = obtenerEntidad(numeroCuenta);
        return new SaldoResponse(cuenta.getNumeroCuenta(), cuenta.getSaldo());
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> buscar(Long clienteId, EstatusCuenta estatus) {
        List<Cuenta> cuentas;
        if (clienteId != null) {
            if (!clienteRepository.existsById(clienteId)) throw new ClienteNoEncontradoException();
            cuentas = (estatus != null)
                    ? cuentaRepository.findByClienteIdAndEstatus(clienteId, estatus)
                    : cuentaRepository.findByClienteId(clienteId);
        } else if (estatus != null) {
            cuentas = cuentaRepository.findByEstatus(estatus);
        } else {
            throw new ErrorValidacionException("Debe indicar clienteId o estatus.");
        }
        return cuentas.stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public CuentaResponse crear(CuentaCreacionRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(ClienteNoEncontradoException::new);
        if (!cliente.getActivo()) throw new ClienteInactivoException();

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(cuentaRepository.generarNumeroCuenta());
        cuenta.setSaldo(request.getSaldoInicial() != null ? request.getSaldoInicial() : saldoInicialPorDefecto);
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        return aRespuesta(cuentaRepository.save(cuenta));
    }

    @Transactional
    public CuentaResponse actualizarEstatus(String numeroCuenta, EstatusCuenta nuevo) {
        Cuenta cuenta = obtenerEntidad(numeroCuenta);
        if (nuevo == EstatusCuenta.ACTIVA && !cuenta.getCliente().getActivo()) {
            throw new ClienteInactivoException();
        }
        cuenta.setEstatus(nuevo);
        return aRespuesta(cuenta);
    }

    private Cuenta obtenerEntidad(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(CuentaNoEncontradaException::new);
    }

    private CuentaResponse aRespuesta(Cuenta c) {
        return CuentaResponse.builder()
                .numeroCuenta(c.getNumeroCuenta())
                .saldo(c.getSaldo())
                .estatus(c.getEstatus())
                .fechaApertura(c.getFechaApertura())
                .clienteId(c.getCliente().getId())
                .build();
    }
}
