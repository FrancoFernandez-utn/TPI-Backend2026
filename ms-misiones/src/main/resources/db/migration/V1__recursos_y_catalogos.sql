-- Esquema propio de ms-misiones: capacidad operativa (pilotos y drones) y catalogos.
-- No hay claves foraneas hacia el esquema de ms-campos.

CREATE TABLE piloto (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_sub    VARCHAR(64)  NOT NULL UNIQUE,
    licencia_nro   VARCHAR(40)  NOT NULL,
    licencia_vence DATE         NOT NULL,
    horas_vuelo    NUMERIC(8, 1) NOT NULL CHECK (horas_vuelo >= 0),
    estado         VARCHAR(20)  NOT NULL,
    creado_en      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE habilitacion (
    piloto_id   BIGINT      NOT NULL REFERENCES piloto (id),
    tipo_mision VARCHAR(30) NOT NULL,
    PRIMARY KEY (piloto_id, tipo_mision)
);

CREATE TABLE modelo_dron (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            VARCHAR(80)   NOT NULL UNIQUE,
    capacidad_l       NUMERIC(6, 1) NOT NULL CHECK (capacidad_l >= 0),
    ha_por_hora       NUMERIC(6, 2) NOT NULL CHECK (ha_por_hora > 0),
    ha_por_vuelo      NUMERIC(6, 2) NOT NULL CHECK (ha_por_vuelo > 0),
    limite_rafaga_kmh NUMERIC(5, 1) NOT NULL CHECK (limite_rafaga_kmh > 0),
    tiene_camara      BOOLEAN       NOT NULL
);

CREATE TABLE dron (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    modelo_id      BIGINT      NOT NULL REFERENCES modelo_dron (id),
    matricula      VARCHAR(30) NOT NULL UNIQUE,
    registro_vence DATE        NOT NULL,
    seguro_vence   DATE        NOT NULL,
    estado         VARCHAR(20) NOT NULL,
    creado_en      TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_dron_modelo ON dron (modelo_id);

CREATE TABLE producto (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         VARCHAR(80) NOT NULL UNIQUE,
    intervalo_dias INTEGER     NOT NULL CHECK (intervalo_dias >= 0)
);

-- Etapas del cultivo en las que el producto no puede aplicarse (RN-09).
CREATE TABLE producto_etapa_prohibida (
    producto_id BIGINT      NOT NULL REFERENCES producto (id),
    etapa       VARCHAR(30) NOT NULL,
    PRIMARY KEY (producto_id, etapa)
);

-- Umbrales climaticos por tipo de mision. Una variable sin fila no tiene restriccion para ese tipo.
CREATE TABLE umbral_clima (
    tipo_mision   VARCHAR(30)   NOT NULL,
    variable      VARCHAR(20)   NOT NULL,
    minimo        NUMERIC(7, 2),
    min_inclusivo BOOLEAN       NOT NULL,
    maximo        NUMERIC(7, 2),
    max_inclusivo BOOLEAN       NOT NULL,
    PRIMARY KEY (tipo_mision, variable),
    CHECK (minimo IS NOT NULL OR maximo IS NOT NULL),
    CHECK (minimo IS NULL OR maximo IS NULL OR minimo <= maximo)
);

-- Valores de la tabla de la seccion 5.3 del dominio. "a" y "entre" son inclusivos;
-- "menor a" y "mayor a" son estrictos. 0 mm de lluvia se modela como maximo 0 inclusivo.
INSERT INTO umbral_clima (tipo_mision, variable, minimo, min_inclusivo, maximo, max_inclusivo) VALUES
    ('PULVERIZACION',     'VIENTO',        3,    true,  15,   true),
    ('PULVERIZACION',     'RAFAGA',        NULL, false, 22,   false),
    ('PULVERIZACION',     'TEMPERATURA',   NULL, false, 30,   false),
    ('PULVERIZACION',     'HUMEDAD',       50,   false, NULL, false),
    ('PULVERIZACION',     'PRECIPITACION', NULL, false, 0,    true),
    ('MONITOREO',         'VIENTO',        NULL, false, 25,   false),
    ('MONITOREO',         'RAFAGA',        NULL, false, 40,   false),
    ('MONITOREO',         'TEMPERATURA',   0,    true,  40,   true),
    ('MONITOREO',         'PRECIPITACION', NULL, false, 0.2,  false),
    ('MONITOREO',         'NUBOSIDAD',     NULL, false, 40,   false),
    ('SIEMBRA_COBERTURA', 'VIENTO',        NULL, false, 20,   false),
    ('SIEMBRA_COBERTURA', 'RAFAGA',        NULL, false, 30,   false),
    ('SIEMBRA_COBERTURA', 'TEMPERATURA',   0,    true,  38,   true),
    ('SIEMBRA_COBERTURA', 'PRECIPITACION', NULL, false, 1,    false);
