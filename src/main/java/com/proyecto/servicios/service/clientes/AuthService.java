package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.LoginResponse;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.security.JwtService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private String hashFicticio;

    @PostConstruct
    void iniciar() {
        hashFicticio = passwordEncoder.encode("ContrasenaFicticia#1");
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Inicio de autenticación");
        String correo = request.getCorreo().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

        // Se compara siempre contra algún hash, para que el tiempo de respuesta no delate si el usuario existe
        String hash = (usuario != null) ? usuario.getPasswordHash() : hashFicticio;
        boolean coincide = passwordEncoder.matches(request.getPassword(), hash);

        if (usuario == null || !coincide) {
            log.warn("Autenticación fallida: credenciales inválidas");
            throw new CredencialesInvalidasException();
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Autenticación denegada: usuario inactivo. usuarioId={}", usuario.getId());
            throw new UsuarioInactivoException();
        }

        LoginResponse respuesta = new LoginResponse();
        respuesta.setCodigo(200);
        respuesta.setMensaje("Autenticación exitosa.");
        respuesta.setToken(jwtService.generar(usuario));
        respuesta.setTipo("Bearer");
        respuesta.setExpiraEnSegundos(jwtService.segundosDeVigencia());
        log.info("Fin de autenticación. usuarioId={}", usuario.getId());
        return respuesta;
    }
}
