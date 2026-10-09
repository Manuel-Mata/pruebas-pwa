package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Nacionalidad;
import com.proyecto.servicios.enums.Pais;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.util.PasswordSegura;
import com.proyecto.servicios.util.Patrones;
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
    @Pattern(regexp = Patrones.NOMBRE, message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = Patrones.CURP, flags = Pattern.Flag.CASE_INSENSITIVE, message = "La CURP no tiene un formato válido")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = Patrones.RFC, flags = Pattern.Flag.CASE_INSENSITIVE, message = "El RFC no tiene un formato válido (12 o 13 caracteres)")
    private String rfc;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento; // La validación de mayoría de edad se hará en el Servicio con Clock

    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;

    @NotNull(message = "La nacionalidad es obligatoria")
    private Nacionalidad nacionalidad;

    @NotNull(message = "El estado civil es obligatorio")
    private EstadoCivil estadoCivil;

    // Contacto
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo es inválido", regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")
    @Size(max = 100, message = "El correo no debe exceder los 100 caracteres")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono móvil debe tener 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono alterno debe tener 10 dígitos")
    private String telefonoAlterno;

    // Información Laboral
    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual admite hasta 10 enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 50, message = "La ocupación debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La ocupación contiene caracteres no permitidos")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa debe tener máximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La empresa contiene caracteres no permitidos")
    private String empresa;

    // Domicilio
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = Patrones.CP, message = "El código postal debe tener 5 dígitos")
    private String cp;

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle debe tener máximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La calle solo admite letras, números y . , # ° ' / & ( ) -")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 20, message = "El número exterior debe tener máximo 20 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "El número exterior contiene caracteres no permitidos")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior debe tener máximo 20 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "El número interior contiene caracteres no permitidos")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia debe tener máximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La colonia contiene caracteres no permitidos")
    private String colonia;

    @Size(max = 50, message = "El municipio debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "El municipio contiene caracteres no permitidos")
    private String municipio;

    @Size(max = 50, message = "El estado debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "El estado contiene caracteres no permitidos")
    private String estado;
    
    private Pais pais; // Opcional, el servicio asignará MEXICO si es nulo

    // Credenciales
    @NotBlank(message = "La contraseña es obligatoria")
    @PasswordSegura
    private String password; // La validación de seguridad (regex) se hará en el Servicio para arrojar ContrasenaInvalidaException
}
