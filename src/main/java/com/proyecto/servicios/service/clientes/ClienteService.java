package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.enums.Pais;
import com.proyecto.servicios.exception.onboarding.*;
import com.proyecto.servicios.model.ClienteDetalleResponse;
import com.proyecto.servicios.model.ClienteRegistroRequest;
import com.proyecto.servicios.model.ClienteRegistroResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.CuentaRepository;
import com.proyecto.servicios.repositorys.clientes.DomicilioRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.util.PasswordSeguraValidator;
import com.proyecto.servicios.util.TextoUtil;
import com.proyecto.servicios.service.catalogos.CodigoPostalService;
import com.proyecto.servicios.model.externo.CodigoPostalResponse;
import com.proyecto.servicios.exception.catalogos.CodigoPostalNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final ClientePersistenciaService persistenciaService;
    private final CodigoPostalService codigoPostalService;
    private final Clock clock; // Permite testing de fechas

    @Value("${postali.validacion-obligatoria:true}")
    private boolean validacionPostalObligatoria;

    public ClienteRegistroResponse registrarCliente(ClienteRegistroRequest request) {
        log.info("Iniciando registro de cliente");

        // 1. Normalización
        normalizarDatos(request);

        // 2. Validaciones de Negocio
        validarNegocio(request);

        // 3. Validar Código Postal (Postali)
        validarCodigoPostal(request);

        // 4. Persistir todo en una sola transacción
        return persistenciaService.guardarTodo(request);
    }

    private void validarCodigoPostal(ClienteRegistroRequest request) {
        if (!validacionPostalObligatoria) {
            if (esVacio(request.getEstado()) || esVacio(request.getMunicipio())) {
                throw new ErrorValidacionException(
                    "Estado y municipio son obligatorios cuando la validación postal está desactivada.");
            }
            return;
        }
        CodigoPostalResponse cp;
        try {
            cp = codigoPostalService.consultar(request.getCp());
        } catch (CodigoPostalNoEncontradoException e) {
            throw new ErrorValidacionException("El código postal no existe en el catálogo.");
        }
        String buscada = TextoUtil.normalizar(request.getColonia());
        boolean coincide = cp.getColonias().stream()
                .map(TextoUtil::normalizar)
                .anyMatch(buscada::equals);
        if (!coincide) {
            throw new ErrorValidacionException("La colonia no pertenece al código postal indicado.");
        }
        request.setEstado(cp.getEstado());
        request.setMunicipio(cp.getMunicipio());
    }

    private boolean esVacio(String s) { return s == null || s.isBlank(); }

    @Transactional(readOnly = true)
    public ClienteDetalleResponse obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(ClienteNoEncontradoException::new);
        
        Domicilio domicilio = domicilioRepository.findByClienteId(id).orElse(null);
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);

        return mapearADetalle(cliente, domicilio, cuentas);
    }

    @Transactional
    public void darDeBaja(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(ClienteNoEncontradoException::new);

        if (!cliente.getActivo()) {
            throw new ClienteInactivoException();
        }

        cliente.setActivo(false);
        cliente.setFechaBaja(OffsetDateTime.now(clock));
        clienteRepository.save(cliente);

        Usuario usuario = usuarioRepository.findByClienteId(id)
                .orElseThrow(UsuarioNoEncontradoException::new);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        for (Cuenta cuenta : cuentas) {
            cuenta.setEstatus(EstatusCuenta.INACTIVA);
        }
        cuentaRepository.saveAll(cuentas);
    }

    private ClienteDetalleResponse mapearADetalle(Cliente cliente, Domicilio domicilio, List<Cuenta> cuentas) {
        ClienteDetalleResponse.DomicilioResponse domRes = null;
        if (domicilio != null) {
            domRes = ClienteDetalleResponse.DomicilioResponse.builder()
                    .calle(domicilio.getCalle())
                    .numeroExterior(domicilio.getNumeroExterior())
                    .numeroInterior(domicilio.getNumeroInterior())
                    .colonia(domicilio.getColonia())
                    .municipio(domicilio.getMunicipio())
                    .estado(domicilio.getEstado())
                    .pais(domicilio.getPais())
                    .cp(domicilio.getCp())
                    .build();
        }

        List<ClienteDetalleResponse.CuentaResponse> cuentasRes = cuentas.stream().map(c -> 
            ClienteDetalleResponse.CuentaResponse.builder()
                .numeroCuenta(c.getNumeroCuenta())
                .saldo(c.getSaldo())
                .estatus(c.getEstatus().name())
                .build()
        ).toList();

        return ClienteDetalleResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlterno(cliente.getTelefonoAlterno())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .correo(cliente.getCorreo())
                .ocupacion(cliente.getOcupacion())
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual())
                .activo(cliente.getActivo())
                .fechaRegistro(cliente.getFechaRegistro())
                .fechaBaja(cliente.getFechaBaja())
                .domicilio(domRes)
                .cuentas(cuentasRes)
                .build();
    }

    private void normalizarDatos(ClienteRegistroRequest request) {
        request.setCurp(request.getCurp().trim().toUpperCase());
        request.setRfc(request.getRfc().trim().toUpperCase());
        request.setCorreo(request.getCorreo().trim().toLowerCase());
        
        if (request.getPais() == null) {
            request.setPais(Pais.MEXICO);
        }
    }

    private void validarNegocio(ClienteRegistroRequest request) {
        // Validar Edad
        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now(clock)).getYears();
        if (edad < 18) {
            throw new ErrorValidacionException("El cliente debe tener al menos 18 años.");
        }

        // Validar Contraseña (uso explícito de la excepción solicitada)
        if (!PasswordSeguraValidator.esValida(request.getPassword())) {
            throw new ContrasenaInvalidaException();
        }

        // Validar Duplicados
        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicadaException();
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicadoException();
        }
        if (clienteRepository.existsByCorreo(request.getCorreo()) || usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoDuplicadoException();
        }
    }
}
