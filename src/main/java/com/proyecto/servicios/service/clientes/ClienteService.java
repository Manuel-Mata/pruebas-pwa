package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.entity.clientes.Domicilio;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.enums.Pais;
import com.proyecto.servicios.exception.onboarding.*;
import com.proyecto.servicios.model.ClienteActualizacionRequest;
import com.proyecto.servicios.model.ClienteDetalleResponse;
import com.proyecto.servicios.model.ClienteRegistroRequest;
import com.proyecto.servicios.model.ClienteRegistroResponse;
import com.proyecto.servicios.model.ClienteFiltro;
import com.proyecto.servicios.model.ClienteResumenResponse;
import com.proyecto.servicios.model.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.Locale;

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

    public record ResultadoPostal(CodigoPostalResponse cp, String coloniaOficial) {}

    private void validarCodigoPostal(ClienteRegistroRequest request) {
        if (!validacionPostalObligatoria) {
            if (esVacio(request.getEstado()) || esVacio(request.getMunicipio())) {
                throw new ErrorValidacionException(
                    "Estado y municipio son obligatorios cuando la validación postal está desactivada.");
            }
            return;
        }
        ResultadoPostal res = validarColonia(request.getCp(), request.getColonia());
        request.setColonia(res.coloniaOficial());
        request.setEstado(res.cp().getEstado());
        request.setMunicipio(res.cp().getMunicipio());
    }

    private ResultadoPostal validarColonia(String cp, String colonia) {
        CodigoPostalResponse respuesta;
        try {
            respuesta = codigoPostalService.consultar(cp);
        } catch (CodigoPostalNoEncontradoException e) {
            throw new ErrorValidacionException("El código postal no existe en el catálogo.");
        }
        String buscada = TextoUtil.normalizar(colonia);
        String oficial = respuesta.getColonias().stream()
                .filter(c -> TextoUtil.normalizar(c).equals(buscada))
                .findFirst()
                .orElseThrow(() -> new ErrorValidacionException("La colonia no pertenece al código postal indicado."));
        return new ResultadoPostal(respuesta, oficial);
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

    public ClienteDetalleResponse actualizar(Long id, ClienteActualizacionRequest r) {
        if (!r.tieneCambios()) {
            throw new ErrorValidacionException("Debe enviar al menos un campo para actualizar.");
        }
        if (r.getCorreo() != null) r.setCorreo(r.getCorreo().trim().toLowerCase());
        if (r.getFechaNacimiento() != null) validarMayoriaDeEdad(r.getFechaNacimiento());

        ResultadoPostal resultadoPostal = null;
        if (validacionPostalObligatoria && (r.getCp() != null || r.getColonia() != null)) {
            Domicilio domActual = domicilioRepository.findByClienteId(id)
                    .orElseThrow(ClienteNoEncontradoException::new);
            String cpAValidar = r.getCp() != null ? r.getCp() : domActual.getCp();
            String colAValidar = r.getColonia() != null ? r.getColonia() : domActual.getColonia();
            resultadoPostal = validarColonia(cpAValidar, colAValidar);
        }

        persistenciaService.aplicarActualizacion(id, r, resultadoPostal);
        return obtenerPorId(id);
    }

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("id", "nombre", "apellidoPaterno", "apellidoMaterno", "fechaRegistro", "activo");

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResumenResponse> buscar(ClienteFiltro f, Pageable pageable) {
        log.info("Inicio de búsqueda de clientes");
        com.proyecto.servicios.util.OrdenUtil.validar(pageable, CAMPOS_ORDENABLES);

        if (f.fechaDesde() != null && f.fechaHasta() != null && f.fechaDesde().isAfter(f.fechaHasta())) {
            throw new ErrorValidacionException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        ZoneId zona = clock.getZone();
        OffsetDateTime desde = f.fechaDesde() == null ? null
                : f.fechaDesde().atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime limite = f.fechaHasta() == null ? null
                : f.fechaHasta().plusDays(1).atStartOfDay(zona).toOffsetDateTime();

        Specification<Cliente> spec = Specification.allOf(
                ClienteSpecification.contiene("nombre", limpiar(f.nombre())),
                ClienteSpecification.contiene("apellidoPaterno", limpiar(f.apellidoPaterno())),
                ClienteSpecification.contiene("apellidoMaterno", limpiar(f.apellidoMaterno())),
                ClienteSpecification.igual("curp", mayusculas(limpiar(f.curp()))),
                ClienteSpecification.igual("rfc", mayusculas(limpiar(f.rfc()))),
                ClienteSpecification.igual("correo", minusculas(limpiar(f.correo()))),
                ClienteSpecification.igual("activo", f.activo()),
                ClienteSpecification.registradoDesde(desde),
                ClienteSpecification.registradoAntesDe(limite),
                ClienteSpecification.conNumeroCuenta(limpiar(f.numeroCuenta())));

        Page<ClienteResumenResponse> pagina =
                clienteRepository.findAll(spec, pageable).map(this::aResumen);
        log.info("Fin de búsqueda de clientes. total={}", pagina.getTotalElements());
        return PaginaResponse.de(pagina);
    }

    private ClienteResumenResponse aResumen(Cliente c) {
        return ClienteResumenResponse.builder()
                .id(c.getId())
                .nombreCompleto(TextoUtil.nombreCompleto(
                        c.getNombre(), c.getSegundoNombre(), c.getApellidoPaterno(), c.getApellidoMaterno()))
                .curp(c.getCurp()).rfc(c.getRfc()).correo(c.getCorreo())
                .telefonoMovil(c.getTelefonoMovil())
                .activo(c.getActivo()).fechaRegistro(c.getFechaRegistro())
                .build();
    }

    private static String limpiar(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static String mayusculas(String s) { return s == null ? null : s.toUpperCase(Locale.ROOT); }
    private static String minusculas(String s) { return s == null ? null : s.toLowerCase(Locale.ROOT); }

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
        validarMayoriaDeEdad(request.getFechaNacimiento());

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

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now(clock)).getYears();
        if (edad < 18) {
            throw new ErrorValidacionException("El cliente debe tener al menos 18 años.");
        }
    }
}
