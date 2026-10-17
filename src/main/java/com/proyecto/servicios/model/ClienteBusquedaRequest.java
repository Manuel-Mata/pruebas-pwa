package com.proyecto.servicios.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class ClienteBusquedaRequest {

    @Positive(message = "El id debe ser mayor a cero")
    private Long id;

    @Size(max = 50, message = "El nombre admite máximo 50 caracteres")
    private String nombre;

    @Size(max = 50, message = "El apellido paterno admite máximo 50 caracteres")
    private String apellidoPaterno;

    @Size(max = 50, message = "El apellido materno admite máximo 50 caracteres")
    private String apellidoMaterno;

    @Size(max = 18, message = "La CURP admite máximo 18 caracteres")
    private String curp;

    @Size(max = 13, message = "El RFC admite máximo 13 caracteres")
    private String rfc;

    @Size(max = 100, message = "El correo admite máximo 100 caracteres")
    private String correo;

    private Boolean activo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaHasta;

    @Size(max = 10, message = "El número de cuenta admite máximo 10 caracteres")
    @Pattern(regexp = "^[0-9]*$", message = "El número de cuenta solo debe contener dígitos")
    private String numeroCuenta;

    @Min(value = 0, message = "La página no puede ser menor a 0")
    private Integer pagina = 0;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 100, message = "El tamaño de página no puede exceder 100")
    private Integer tamano = 10;

    private String ordenarPor = "id";

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "La dirección de ordenamiento debe ser ASC o DESC")
    private String direccion = "ASC";
}
