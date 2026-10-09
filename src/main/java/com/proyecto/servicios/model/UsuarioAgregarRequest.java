package com.proyecto.servicios.model;

import com.proyecto.servicios.util.PasswordSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UsuarioAgregarRequest {
    @NotNull(message = "El clienteId es obligatorio")
    @Positive(message = "El clienteId debe ser mayor a cero")
    private Long clienteId;

    @NotBlank(message = "La contraseña es obligatoria")
    @PasswordSegura
    private String password;
}
