package com.proyecto.servicios.model;

import com.proyecto.servicios.util.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodigoPostalRequest {

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = Patrones.CP, message = "El código postal debe tener 5 dígitos")
    private String cp;
}
