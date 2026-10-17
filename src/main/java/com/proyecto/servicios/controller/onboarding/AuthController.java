package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.LoginRequest;
import com.proyecto.servicios.model.LoginResponse;
import com.proyecto.servicios.service.clientes.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<GenericResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(GenericResponse.exito("Autenticación exitosa.", response));
    }
}
