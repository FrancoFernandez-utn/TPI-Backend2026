-- Esquema propio de ms-campos. No hay claves foraneas hacia otros esquemas.

CREATE TABLE establecimiento (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    productor_sub VARCHAR(64)  NOT NULL,
    nombre        VARCHAR(120) NOT NULL,
    creado_en     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_establecimiento_productor ON establecimiento (productor_sub);

CREATE TABLE lote (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    establecimiento_id BIGINT        NOT NULL REFERENCES establecimiento (id),
    nombre             VARCHAR(120)  NOT NULL,
    lat                NUMERIC(9, 6) NOT NULL CHECK (lat BETWEEN -90 AND 90),
    lon                NUMERIC(9, 6) NOT NULL CHECK (lon BETWEEN -180 AND 180),
    hectareas          NUMERIC(10, 2) NOT NULL CHECK (hectareas > 0),
    estado             VARCHAR(20)   NOT NULL,
    creado_en          TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_lote_establecimiento ON lote (establecimiento_id);

-- Historial de cultivo y etapa: la fila mas reciente de cada lote es la etapa actual.
CREATE TABLE cultivo_lote (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lote_id BIGINT      NOT NULL REFERENCES lote (id),
    cultivo VARCHAR(60) NOT NULL,
    etapa   VARCHAR(30) NOT NULL,
    desde   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_cultivo_lote_lote_desde ON cultivo_lote (lote_id, desde DESC);
