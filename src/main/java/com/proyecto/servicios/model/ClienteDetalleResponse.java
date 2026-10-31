package com.proyecto.servicios.model;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Nacionalidad;
import com.proyecto.servicios.enums.Pais;
import com.proyecto.servicios.enums.Sexo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ClienteDetalleResponse {
    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private Sexo sexo;
    private Nacionalidad nacionalidad;
    private EstadoCivil estadoCivil;
    private String telefonoMovil;
    private String telefonoAlterno;
    private String curp;
    private String rfc;
    private String correo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private OffsetDateTime fechaRegistro;
    private OffsetDateTime fechaBaja;

    private DomicilioResponse domicilio;
    private List<CuentaResponse> cuentas;

    @Getter
    @Setter
    @Builder
    public static class DomicilioResponse {
        private String calle;
        private String numeroExterior;
        private String numeroInterior;
        private String colonia;
        private String municipio;
        private String estado;
        private Pais pais;
        private String cp;
    }

    @Getter
    @Setter
    @Builder
    public static class CuentaResponse {
        private String numeroCuenta;
        private BigDecimal saldo;
        private String estatus;
    }
}
