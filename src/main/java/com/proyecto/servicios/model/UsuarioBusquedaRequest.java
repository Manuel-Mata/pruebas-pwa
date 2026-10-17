package com.proyecto.servicios.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioBusquedaRequest {

    @Size(max = 100, message = "El correo admite máximo 100 caracteres")
    private String correo;

    private Boolean activo;

    @Positive(message = "El clienteId debe ser mayor a cero")
    private Long clienteId;

    @Min(value = 0, message = "La página no puede ser menor a 0")
    private Integer pagina = 0;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 100, message = "El tamaño de página no puede exceder 100")
    private Integer tamano = 10;

    private String ordenarPor = "id";

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "La dirección de ordenamiento debe ser ASC o DESC")
    private String direccion = "ASC";
}
