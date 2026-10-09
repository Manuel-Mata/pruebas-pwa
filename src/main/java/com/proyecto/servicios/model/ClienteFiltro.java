package com.proyecto.servicios.model;

import java.time.LocalDate;

public record ClienteFiltro(String nombre, String apellidoPaterno, String apellidoMaterno,
                            String curp, String rfc, String correo, Boolean activo,
                            LocalDate fechaDesde, LocalDate fechaHasta, String numeroCuenta) {}
