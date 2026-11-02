-- Datos de demostracion (solo se cargan si Flyway incluye classpath:db/demo).
-- Mision COMPLETADA como dato historico para demostrar RN-10 sin implementar el ciclo de ejecucion (D-02):
-- el Herbicida Historico se aplico en el Lote Historico (900001) hace 2 dias y su intervalo es de 7 dias.
INSERT INTO producto (id, nombre, intervalo_dias) OVERRIDING SYSTEM VALUE
VALUES (900001, 'Herbicida Historico', 7);

INSERT INTO producto_etapa_prohibida (producto_id, etapa) VALUES (900001, 'FLORACION');

INSERT INTO mision (lote_id, productor_sub, tipo, producto_id, receta_nro, estado, inicio_prev, fin_prev,
                    vuelos, duracion_min, fecha_limite, motivos_json, creada_en, actualizada_en)
VALUES (900001, 'a1b2c3d4-0000-4000-8000-000000000002', 'PULVERIZACION', 900001, 'R-HISTORICA', 'COMPLETADA',
        now() - interval '2 days 3 hours', now() - interval '2 days', 1, 180, now() - interval '2 days',
        '[]'::jsonb, now() - interval '3 days', now() - interval '2 days');
