package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.PaginaResponse;
import com.proyecto.servicios.model.UsuarioAgregarRequest;
import com.proyecto.servicios.model.UsuarioResponse;
import com.proyecto.servicios.service.clientes.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/filtro")
    public PaginaResponse<UsuarioResponse> filtro(
            @RequestParam(required = false) @Size(max = 100, message = "El correo admite máximo 100 caracteres") String correo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) @Positive(message = "El clienteId debe ser mayor a cero") Long clienteId,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return usuarioService.buscar(correo, activo, clienteId, pageable);
    }

    @PutMapping("/agregar")
    public ResponseEntity<UsuarioResponse> agregar(@Valid @RequestBody UsuarioAgregarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.agregar(request));
    }
}
