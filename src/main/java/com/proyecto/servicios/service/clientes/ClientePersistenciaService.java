package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.ClienteInactivoException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.model.ClienteActualizacionRequest;
import com.proyecto.servicios.model.ClienteRegistroRequest;
import com.proyecto.servicios.model.ClienteRegistroResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.DomicilioRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ClientePersistenciaService {

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${onboarding.cuenta.saldo-inicial:0.00}")
    private BigDecimal saldoInicial;

    @Transactional
    public ClienteRegistroResponse guardarTodo(ClienteRegistroRequest request) {
        Cliente cliente = guardarCliente(request);
        guardarDomicilio(request, cliente);
        guardarUsuario(request, cliente);
        Cuenta cuenta = guardarCuenta(request, cliente);

        String nombreCompleto = String.join(" ", 
                request.getNombre(), 
                request.getSegundoNombre() == null ? "" : request.getSegundoNombre(),
                request.getApellidoPaterno(), 
                request.getApellidoMaterno()
        ).replaceAll(" +", " ").trim();

        return ClienteRegistroResponse.builder()
                .clienteId(cliente.getId())
                .nombreCompleto(nombreCompleto)
                .numeroCuenta(cuenta.getNumeroCuenta())
                .correo(request.getCorreo())
                .mensaje("Cliente registrado exitosamente.")
                .build();
    }

    private Cliente guardarCliente(ClienteRegistroRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.setCorreo(request.getCorreo());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlterno(request.getTelefonoAlterno());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setActivo(true);
        return clienteRepository.save(cliente);
    }

    private void guardarDomicilio(ClienteRegistroRequest request, Cliente cliente) {
        Domicilio domicilio = new Domicilio();
        domicilio.setCliente(cliente);
        domicilio.setCalle(request.getCalle());
        domicilio.setNumeroExterior(request.getNumeroExterior());
        domicilio.setNumeroInterior(request.getNumeroInterior());
        domicilio.setColonia(request.getColonia());
        domicilio.setMunicipio(request.getMunicipio());
        domicilio.setEstado(request.getEstado());
        domicilio.setPais(request.getPais());
        domicilio.setCp(request.getCp());
        domicilioRepository.save(domicilio);
    }

    private void guardarUsuario(ClienteRegistroRequest request, Cliente cliente) {
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(request.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    private Cuenta guardarCuenta(ClienteRegistroRequest request, Cliente cliente) {
        String numCuenta = cuentaRepository.generarNumeroCuenta();
        
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(numCuenta);
        cuenta.setSaldo(saldoInicial);
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        return cuentaRepository.save(cuenta);
    }

    @Transactional
    public void aplicarActualizacion(Long id, ClienteActualizacionRequest r, ClienteService.ResultadoPostal resultadoPostal) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(ClienteNoEncontradoException::new);
        if (!cliente.getActivo()) throw new ClienteInactivoException();

        if (r.getCorreo() != null && !r.getCorreo().equals(cliente.getCorreo())) {
            if (clienteRepository.existsByCorreo(r.getCorreo()) || usuarioRepository.existsByCorreo(r.getCorreo())) {
                throw new CorreoDuplicadoException();
            }
            cliente.setCorreo(r.getCorreo());
            Usuario usuario = usuarioRepository.findByClienteId(id).orElseThrow();
            usuario.setCorreo(r.getCorreo());
            usuarioRepository.save(usuario);
        }

        aplicar(r.getNombre(), cliente::setNombre);
        aplicar(r.getSegundoNombre(), cliente::setSegundoNombre);
        aplicar(r.getApellidoPaterno(), cliente::setApellidoPaterno);
        aplicar(r.getApellidoMaterno(), cliente::setApellidoMaterno);
        aplicar(r.getFechaNacimiento(), cliente::setFechaNacimiento);
        aplicar(r.getSexo(), cliente::setSexo);
        aplicar(r.getNacionalidad(), cliente::setNacionalidad);
        aplicar(r.getEstadoCivil(), cliente::setEstadoCivil);
        aplicar(r.getTelefonoMovil(), cliente::setTelefonoMovil);
        aplicar(r.getTelefonoAlterno(), cliente::setTelefonoAlterno);
        aplicar(r.getIngresoMensual(), cliente::setIngresoMensual);
        aplicar(r.getOcupacion(), cliente::setOcupacion);
        aplicar(r.getEmpresa(), cliente::setEmpresa);

        clienteRepository.save(cliente);

        if (r.getCp() != null || r.getCalle() != null || r.getNumeroExterior() != null || r.getNumeroInterior() != null || r.getColonia() != null || resultadoPostal != null) {
            Domicilio dom = domicilioRepository.findByClienteId(id).orElseThrow();
            aplicar(r.getCalle(), dom::setCalle);
            aplicar(r.getNumeroExterior(), dom::setNumeroExterior);
            aplicar(r.getNumeroInterior(), dom::setNumeroInterior);
            
            if (resultadoPostal != null) {
                dom.setCp(resultadoPostal.cp().getCp());
                dom.setColonia(resultadoPostal.coloniaOficial());
                dom.setEstado(resultadoPostal.cp().getEstado());
                dom.setMunicipio(resultadoPostal.cp().getMunicipio());
            } else {
                aplicar(r.getCp(), dom::setCp);
                aplicar(r.getColonia(), dom::setColonia);
            }
            domicilioRepository.save(dom);
        }
    }

    private <T> void aplicar(T valor, Consumer<T> setter) {
        if (valor != null) setter.accept(valor);
    }
}
