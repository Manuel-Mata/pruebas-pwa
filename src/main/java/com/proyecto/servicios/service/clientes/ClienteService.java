package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.onboarding.*;
import com.proyecto.servicios.model.ClienteRegistroRequest;
import com.proyecto.servicios.model.ClienteRegistroResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.DomicilioRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock; // Permite testing de fechas

    @Transactional
    public ClienteRegistroResponse registrarCliente(ClienteRegistroRequest request) {
        log.info("Iniciando registro para CURP: {}", request.getCurp());

        // 1. Normalización
        normalizarDatos(request);

        // 2. Validaciones de Negocio
        validarNegocio(request);

        // 3. Crear Cliente
        Cliente cliente = guardarCliente(request);

        // 4. Crear Domicilio
        guardarDomicilio(request, cliente);

        // 5. Crear Usuario
        guardarUsuario(request, cliente);

        // 6. Crear Cuenta
        Cuenta cuenta = guardarCuenta(request, cliente);

        // 7. Retornar Respuesta
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

    private void normalizarDatos(ClienteRegistroRequest request) {
        request.setCurp(request.getCurp().trim().toUpperCase());
        request.setRfc(request.getRfc().trim().toUpperCase());
        request.setCorreo(request.getCorreo().trim().toLowerCase());
        
        if (request.getPais() == null || request.getPais().trim().isEmpty()) {
            request.setPais("México");
        }
    }

    private void validarNegocio(ClienteRegistroRequest request) {
        // Validar Edad
        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now(clock)).getYears();
        if (edad < 18) {
            throw new ErrorValidacionException("El cliente debe ser mayor de 18 años.");
        }

        // Validar Contraseña (Min 8, 1 mayus, 1 minus, 1 num, 1 especial)
        if (!request.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new ContrasenaInvalidaException();
        }

        // Validar Duplicados
        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicadaException();
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicadoException();
        }
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoDuplicadoException();
        }
    }

    private Cliente guardarCliente(ClienteRegistroRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
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
        cuenta.setSaldo(BigDecimal.ZERO);
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        return cuentaRepository.save(cuenta);
    }
}
