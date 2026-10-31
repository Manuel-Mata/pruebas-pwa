CREATE TABLE transacciones (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cuenta_id BIGINT NOT NULL REFERENCES cuentas(id),
    upc VARCHAR(100) NOT NULL UNIQUE,
    id_servicio INT NOT NULL,
    id_producto INT NOT NULL,
    referencia_pago VARCHAR(100),
    monto NUMERIC(14,2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    id_tx_gestopago VARCHAR(50),
    num_autorizacion VARCHAR(50),
    saldo_resultante NUMERIC(14,2),
    comision NUMERIC(14,2),
    mensaje_codigo VARCHAR(10),
    mensaje_texto VARCHAR(255),
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT now(),
    fecha_respuesta TIMESTAMP,
    intentos_confirmacion INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_transacciones_cuenta ON transacciones(cuenta_id);
CREATE INDEX idx_transacciones_estado ON transacciones(estado);
