CREATE TABLE IF NOT EXISTS gestopago_tokens (
    id                  SERIAL PRIMARY KEY,
    id_distribuidor     INTEGER         NOT NULL,
    codigo_dispositivo  VARCHAR(100)    NOT NULL,
    token               TEXT            NOT NULL,
    token_type          VARCHAR(50),
    expires_in          BIGINT,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_gestopago_tokens UNIQUE (id_distribuidor, codigo_dispositivo)
);
-- V2__crear_tablas_onboarding.sql
-- Creación de tablas para el módulo de Onboarding de Clientes con buenas prácticas (alineación de memoria, constraints, tipos precisos)

-- Secuencia para el número de cuenta (empezando en 10 dígitos)
CREATE SEQUENCE seq_numero_cuenta START 1000000000 MAXVALUE 9999999999;

CREATE TABLE clientes (
    -- Tipos fijos grandes de 8 bytes primero (Alineación de memoria)
    id BIGSERIAL,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_baja TIMESTAMPTZ,
    -- Tipos fijos de 4 bytes
    fecha_nacimiento DATE NOT NULL,
    -- Tipos de 1 byte
    activo BOOLEAN NOT NULL DEFAULT true,
    -- Tipos variables o con cabecera (NUMERIC, CHAR, VARCHAR) al final
    ingreso_mensual NUMERIC(12,2) NOT NULL,
    sexo CHAR(1) NOT NULL,
    estado_civil VARCHAR(15) NOT NULL,
    telefono_movil CHAR(10) NOT NULL,
    telefono_alterno CHAR(10),
    curp CHAR(18) NOT NULL,
    rfc VARCHAR(13) NOT NULL,
    correo VARCHAR(100) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    nacionalidad VARCHAR(50) NOT NULL,
    ocupacion VARCHAR(50) NOT NULL,
    empresa VARCHAR(100) NOT NULL,
    
    -- Restricciones
    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo),
    CONSTRAINT ck_clientes_ingreso CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_clientes_sexo CHECK (sexo IN ('H', 'M')),
    CONSTRAINT ck_clientes_civil CHECK (estado_civil IN ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'UNION_LIBRE')),
    CONSTRAINT ck_clientes_tel_movil CHECK (telefono_movil ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_tel_alt CHECK (telefono_alterno IS NULL OR telefono_alterno ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_curp_regex CHECK (curp ~ '^[A-Z0-9]{18}$'),
    CONSTRAINT ck_clientes_rfc_regex CHECK (rfc ~ '^[A-ZÑ&0-9]{12,13}$'),
    CONSTRAINT ck_clientes_correo_lower CHECK (correo = lower(correo))
);

CREATE TABLE usuarios (
    id BIGSERIAL,
    cliente_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT true,
    correo VARCHAR(100) NOT NULL,
    password_hash CHAR(60) NOT NULL,
    
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT ck_usuarios_correo_lower CHECK (correo = lower(correo))
);

CREATE TABLE domicilios (
    id BIGSERIAL,
    cliente_id BIGINT NOT NULL,
    cp CHAR(5) NOT NULL,
    calle VARCHAR(100) NOT NULL,
    numero_exterior VARCHAR(20) NOT NULL,
    numero_interior VARCHAR(20),
    colonia VARCHAR(100) NOT NULL,
    municipio VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    pais VARCHAR(50) NOT NULL DEFAULT 'México',
    
    CONSTRAINT pk_domicilios PRIMARY KEY (id),
    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT ck_domicilios_cp CHECK (cp ~ '^[0-9]{5}$')
);

CREATE TABLE cuentas (
    id BIGSERIAL,
    cliente_id BIGINT NOT NULL,
    fecha_apertura TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saldo NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    numero_cuenta CHAR(10) NOT NULL,
    estatus VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    
    CONSTRAINT pk_cuentas PRIMARY KEY (id),
    CONSTRAINT uq_cuentas_numero UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT ck_cuentas_saldo CHECK (saldo >= 0),
    CONSTRAINT ck_cuentas_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT ck_cuentas_numero_regex CHECK (numero_cuenta ~ '^[0-9]{10}$')
);

-- Índices NO únicos (necesarios para búsquedas y optimización)
CREATE INDEX idx_clientes_fecha_registro ON clientes(fecha_registro);
CREATE INDEX idx_clientes_apellido_pat ON clientes(apellido_paterno);
CREATE INDEX idx_clientes_apellido_mat ON clientes(apellido_materno);
CREATE INDEX idx_clientes_nombre ON clientes(nombre);
CREATE INDEX idx_cuentas_cliente_id ON cuentas(cliente_id);
CREATE INDEX idx_cuentas_estatus ON cuentas(estatus);
ALTER TABLE clientes ADD CONSTRAINT ck_clientes_nacionalidad CHECK (nacionalidad = 'MEXICANA');
ALTER TABLE domicilios ALTER COLUMN pais SET DEFAULT 'MEXICO';
ALTER TABLE domicilios ADD CONSTRAINT ck_domicilios_pais CHECK (pais = 'MEXICO');
ALTER TABLE cuentas DROP CONSTRAINT ck_cuentas_estatus;
DROP INDEX IF EXISTS idx_cuentas_estatus;
ALTER TABLE cuentas ALTER COLUMN estatus DROP DEFAULT;
ALTER TABLE cuentas ALTER COLUMN estatus TYPE BOOLEAN USING (estatus = 'ACTIVA');
ALTER TABLE cuentas RENAME COLUMN estatus TO activa;
ALTER TABLE cuentas ALTER COLUMN activa SET DEFAULT true;
ALTER TABLE usuarios ADD COLUMN intentos_fallidos INT DEFAULT 0 NOT NULL;
