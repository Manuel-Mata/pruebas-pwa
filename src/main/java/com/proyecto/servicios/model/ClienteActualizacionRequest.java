package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Nacionalidad;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.util.Patrones;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

@Getter @Setter
public class ClienteActualizacionRequest {

    @Size(max = 50, message = "El nombre debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El nombre contiene caracteres no permitidos")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El segundo nombre contiene caracteres no permitidos")
    private String segundoNombre;

    @Size(max = 50, message = "El apellido paterno debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido paterno contiene caracteres no permitidos")
    private String apellidoPaterno;

    @Size(max = 50, message = "El apellido materno debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido materno contiene caracteres no permitidos")
    private String apellidoMaterno;

    @Null(message = "La CURP no puede modificarse")
    private String curp;

    @Null(message = "El RFC no puede modificarse")
    private String rfc;

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    private Sexo sexo;
    private Nacionalidad nacionalidad;
    private EstadoCivil estadoCivil;

    @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono móvil debe tener 10 dígitos numéricos")
    private String telefonoMovil;

    @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono alterno debe tener 10 dígitos numéricos")
    private String telefonoAlterno;

    @Email(message = "El formato del correo es inválido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @DecimalMin(value = "0.0", inclusive = false, message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual admite hasta 10 enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    @Size(max = 100, message = "La ocupación debe tener máximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La ocupación contiene caracteres no permitidos")
    private String ocupacion;

    @Size(max = 100, message = "La empresa debe tener máximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La empresa contiene caracteres no permitidos")
    private String empresa;

    @Size(max = 100, message = "La calle debe tener máximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La calle contiene caracteres no permitidos")
    private String calle;

    @Size(max = 20, message = "El número exterior debe tener máximo 20 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "El número exterior contiene caracteres no permitidos")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior debe tener máximo 20 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "El número interior contiene caracteres no permitidos")
    private String numeroInterior;

    @Size(max = 50, message = "La colonia debe tener máximo 50 caracteres")
    @Pattern(regexp = Patrones.TEXTO_LIBRE, message = "La colonia contiene caracteres no permitidos")
    private String colonia;

    @Pattern(regexp = Patrones.CP, message = "El código postal debe tener 5 dígitos")
    private String cp;

    public boolean tieneCambios() {
        return Stream.of(nombre, segundoNombre, apellidoPaterno, apellidoMaterno, fechaNacimiento,
                sexo, nacionalidad, estadoCivil, correo, telefonoMovil, telefonoAlterno,
                ingresoMensual, ocupacion, empresa, cp, calle, numeroExterior, numeroInterior, colonia)
                .anyMatch(Objects::nonNull);
    }
}
