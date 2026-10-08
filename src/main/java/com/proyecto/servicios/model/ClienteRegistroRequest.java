package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstadoCivil;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ClienteRegistroRequest {

    // Datos Personales
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre debe tener máximo 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z0-9]{18}$", message = "La CURP debe tener exactamente 18 caracteres alfanuméricos")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-ZÑ&0-9]{12,13}$", message = "El RFC debe tener 12 o 13 caracteres")
    private String rfc;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento; // La validación de mayoría de edad se hará en el Servicio con Clock

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^[HM]$", message = "El sexo debe ser 'H' o 'M'")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotNull(message = "El estado civil es obligatorio")
    private EstadoCivil estadoCivil;

    // Contacto
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo es inválido", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")
    @Size(max = 100, message = "El correo no debe exceder los 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe tener 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono alterno debe tener 10 dígitos")
    private String telefonoAlterno;

    // Información Laboral
    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    // Domicilio
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe tener 5 dígitos")
    private String cp;

    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    private String numeroExterior;

    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;
    
    private String pais; // Opcional, el servicio asignará "México" si es nulo

    // Credenciales
    @NotBlank(message = "La contraseña es obligatoria")
    private String password; // La validación de seguridad (regex) se hará en el Servicio para arrojar ContrasenaInvalidaException
}
