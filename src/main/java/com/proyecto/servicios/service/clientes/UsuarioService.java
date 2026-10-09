package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.onboarding.ClienteInactivoException;
import com.proyecto.servicios.exception.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.UsuarioYaExisteException;
import com.proyecto.servicios.model.PaginaResponse;
import com.proyecto.servicios.model.UsuarioAgregarRequest;
import com.proyecto.servicios.model.UsuarioResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.util.OrdenUtil;
import com.proyecto.servicios.util.PasswordSeguraValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Set<String> CAMPOS_ORDENABLES = Set.of("id", "correo", "activo", "fechaCreacion");

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> buscar(String correo, Boolean activo, Long clienteId, Pageable pageable) {
        OrdenUtil.validar(pageable, CAMPOS_ORDENABLES);
        String texto = (correo == null || correo.isBlank()) ? null : correo.trim();
        Specification<Usuario> spec = Specification.allOf(
                UsuarioSpecification.correoContiene(texto),
                UsuarioSpecification.activo(activo),
                UsuarioSpecification.deCliente(clienteId));
        return PaginaResponse.de(usuarioRepository.findAll(spec, pageable).map(this::aRespuesta));
    }

    @Transactional
    public UsuarioResponse agregar(UsuarioAgregarRequest request) {
        if (!PasswordSeguraValidator.esValida(request.getPassword())) {
            throw new ContrasenaInvalidaException();
        }
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(ClienteNoEncontradoException::new);
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException();
        }
        if (usuarioRepository.findByClienteId(cliente.getId()).isPresent()) {
            throw new UsuarioYaExisteException();
        }
        if (usuarioRepository.existsByCorreo(cliente.getCorreo())) {
            throw new CorreoDuplicadoException();
        }
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(cliente.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        return aRespuesta(usuarioRepository.save(usuario));
    }

    private UsuarioResponse aRespuesta(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .clienteId(u.getCliente().getId())
                .correo(u.getCorreo())
                .activo(u.getActivo())
                .fechaCreacion(u.getFechaCreacion())
                .fechaActualizacion(u.getFechaActualizacion())
                .build();
    }
}
