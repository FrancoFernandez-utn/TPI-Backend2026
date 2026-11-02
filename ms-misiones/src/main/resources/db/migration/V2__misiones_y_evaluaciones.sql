-- Misiones y evaluaciones climaticas.
-- lote_id y productor_sub son referencias logicas a ms-campos: no hay clave foranea entre esquemas.

CREATE TABLE mision (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lote_id          BIGINT      NOT NULL,
    productor_sub    VARCHAR(64) NOT NULL,
    tipo             VARCHAR(30) NOT NULL,
    producto_id      BIGINT REFERENCES producto (id),
    receta_nro       VARCHAR(40),
    piloto_id        BIGINT REFERENCES piloto (id),
    dron_id          BIGINT REFERENCES dron (id),
    estado           VARCHAR(30) NOT NULL,
    inicio_prev      TIMESTAMPTZ,
    fin_prev         TIMESTAMPTZ,
    vuelos           INTEGER,
    duracion_min     INTEGER,
    fecha_limite     TIMESTAMPTZ NOT NULL,
    contexto_json    JSONB,
    motivos_json     JSONB       NOT NULL DEFAULT '[]',
    creada_en        TIMESTAMPTZ NOT NULL,
    actualizada_en   TIMESTAMPTZ NOT NULL,
    CHECK (inicio_prev IS NULL OR fin_prev > inicio_prev)
);

-- Consultas de solapamiento de RN-03 y RN-06, y de historial de RN-10.
CREATE INDEX ix_mision_dron_estado ON mision (dron_id, estado);
CREATE INDEX ix_mision_piloto_estado ON mision (piloto_id, estado);
CREATE INDEX ix_mision_lote_producto_estado ON mision (lote_id, producto_id, estado);
CREATE INDEX ix_mision_productor ON mision (productor_sub);

-- Evaluacion climatica que justifico la decision: datos usados, ventanas encontradas y resultado.
CREATE TABLE evaluacion_mision (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mision_id      BIGINT      NOT NULL REFERENCES mision (id),
    consultada_en  TIMESTAMPTZ NOT NULL,
    fuente         VARCHAR(30) NOT NULL,
    resultado      VARCHAR(20) NOT NULL,
    variables_json JSONB       NOT NULL,
    ventanas_json  JSONB       NOT NULL,
    motivos_json   JSONB       NOT NULL DEFAULT '[]'
);

CREATE INDEX ix_evaluacion_mision ON evaluacion_mision (mision_id, consultada_en DESC);
