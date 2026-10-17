package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PaginaResponse;
import com.proyecto.servicios.model.UsuarioAgregarRequest;
import com.proyecto.servicios.model.UsuarioBusquedaRequest;
import com.proyecto.servicios.model.UsuarioResponse;
import com.proyecto.servicios.service.clientes.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping({"/buscar", "/filtro"})
    public ResponseEntity<GenericResponse<PaginaResponse<UsuarioResponse>>> buscar(
            @Valid @RequestBody(required = false) UsuarioBusquedaRequest request) {
        UsuarioBusquedaRequest req = (request != null) ? request : new UsuarioBusquedaRequest();
        PaginaResponse<UsuarioResponse> pagina = usuarioService.buscar(req);
        return ResponseEntity.ok(GenericResponse.exito("Búsqueda de usuarios realizada exitosamente.", pagina));
    }

    @PutMapping("/agregar")
    public ResponseEntity<GenericResponse<UsuarioResponse>> agregar(@Valid @RequestBody UsuarioAgregarRequest request) {
        UsuarioResponse response = usuarioService.agregar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito(HttpStatus.CREATED.value(), "Usuario agregado exitosamente.", response));
    }
}
