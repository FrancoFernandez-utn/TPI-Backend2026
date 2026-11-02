-- Datos de demostracion (solo se cargan si Flyway incluye classpath:db/demo).
-- Lote de productor1 con historial de aplicaciones, para demostrar RN-10 (decision D-02).
-- Los ids altos evitan choques con los que genera la aplicacion.
INSERT INTO establecimiento (id, productor_sub, nombre, creado_en) OVERRIDING SYSTEM VALUE
VALUES (900001, 'a1b2c3d4-0000-4000-8000-000000000002', 'Estancia Demo', now());

INSERT INTO lote (id, establecimiento_id, nombre, lat, lon, hectareas, estado, creado_en) OVERRIDING SYSTEM VALUE
VALUES (900001, 900001, 'Lote Historico', -31.380000, -64.120000, 25.00, 'ACTIVO', now());

INSERT INTO cultivo_lote (lote_id, cultivo, etapa, desde)
VALUES (900001, 'soja', 'VEGETATIVO', now() - interval '20 days');
