# Decisiones del MVP y trazabilidad

Este documento resume las decisiones con las que se implementó el MVP y dónde está cada regla en el código y en las pruebas. El análisis completo y la discusión de cada decisión están en el documento de análisis del equipo, pestaña "Diseño MVP definitivo (v3)"

## Alcance

Se implementa el **MVP de la sección 17** del dominio:

- gestión de establecimientos y lotes;
- gestión de pilotos y drones;
- solicitud con validaciones;
- evaluación climática con Open-Meteo;
- búsqueda de ventanas;
- autorización o rechazo con motivos;
- seguridad y despliegue con Compose.

Además se incorpora una **cancelación mínima** (D-06), que libera el piloto y el dron reservados.

**Queda fuera:**

- zonas sensibles (RN-11, RN-12, RN-13);
- reevaluación, reprogramación y revocación (RN-17, RN-18);
- ejecución y bitácora (RN-19, RN-20);
- mantenimiento automático;
- notificaciones, email y métricas.

## Decisiones

| ID | Decisión |
| --- | --- |
| D-01 | Zonas sensibles fuera del MVP (la sección 17 las clasifica como Recomendado) |
| D-02 | RN-10 se demuestra con una misión COMPLETADA cargada como dato histórico (`db/demo`) |
| D-03 | Piloto y dron se asignan automáticamente en la misma solicitud; la misión queda PROGRAMADA (sección 8.1 del dominio) |
| D-04 | Sin control de horas desde el último mantenimiento ni paso automático a EN_MANTENIMIENTO |
| D-05 | Umbrales climáticos en la tabla `umbral_clima`, sembrados con la sección 5.3 y editables por ADMIN |
| D-06 | `POST /api/misiones/{id}/cancelar`: AUTORIZADA o PROGRAMADA pasan a CANCELADA |
| D-07 | `GET /api/productos` para cualquier usuario autenticado; escritura solo ADMIN |
| D-08 | SIEMBRA_COBERTURA no tiene requisito de equipamiento: RN-05 solo lo define para PULVERIZACION y MONITOREO |
| D-10 | Semántica temporal de Open-Meteo según su documentación oficial (ver abajo) |
| D-11 | RN-03 es un tope diario por piloto: una misión de más de 6 h termina en SIN_PILOTO_DISPONIBLE, sin un motivo propio |
| D-12 | Catálogo cerrado de etapas, decisión del equipo: SIEMBRA, EMERGENCIA, VEGETATIVO, FLORACION, FRUCTIFICACION, MADUREZ |
| D-13 | Modelos de dron con `POST` (ADMIN) y `GET` (autenticado) |
| D-14 | Proveedor climático simulado, con escenarios por coordenadas y generado a partir de "hoy" |
| D-15 | Bloqueo pesimista (`SELECT … FOR UPDATE`) de drones y pilotos candidatos, siempre en orden por id |
| D-16 | Categorías separadas: 400 entrada inválida, 404 inexistente o ajeno, 409 conflicto de estado, 201 RECHAZADA para reglas de negocio, 503 dependencia caída |
| D-18 | Estados de misión: SOLICITADA, AUTORIZADA, PROGRAMADA, RECHAZADA, CANCELADA y COMPLETADA (solo histórico) |
| D-19 | Los estados iniciales los fija el sistema: piloto ACTIVO, dron OPERATIVO, lote ACTIVO |

### Decisiones técnicas

- **Versiones.** Spring Boot 4.0.9 con Spring Cloud 2025.1.4, sobre Java 21. Es la combinación con soporte libre durante la entrega; Spring Boot 3.5 lo perdió en junio de 2026.
- **JAR común como proyecto independiente.** No hay pom agregador: `agrodrones-commons` se instala en `.m2` y los servicios lo declaran como dependencia. En Docker, la etapa de build lo instala antes de empaquetar cada servicio.
- **Seguridad en dos capas.** El Gateway valida el JWT en el borde y bloquea `/internal/**`. Cada servicio lo vuelve a validar y aplica `@PreAuthorize` y la regla de propiedad (comparación del `sub`). La cadena de seguridad de los servicios está en `commons` porque era idéntica en ambos.
- **Llamada interna.** `ms-misiones → ms-campos` usa un token propio de client credentials con rol SERVICIO, no el token del usuario.
- **Snapshot del contexto.** La misión guarda una copia del contexto del lote con el que se decidió, para auditoría.
- **Evaluación auditada.** Cada consulta climática guarda las series usadas, las ventanas encontradas y el resultado.
- **Resiliencia de Open-Meteo.** Timeout de conexión y de lectura, 2 reintentos ante errores transitorios (un 400 no se reintenta) y circuit breaker de Resilience4j. Sin respuesta, la solicitud se rechaza con 503: falla cerrada.
- **Sin bloqueos durante esperas de red.** Las llamadas a ms-campos y a Open-Meteo ocurren antes de abrir la transacción de asignación.

### Semántica temporal de Open-Meteo (D-10)

Según la documentación oficial, la precipitación de cada marca horaria es la **suma de la hora anterior** y la ráfaga es el **máximo de la hora anterior**. Viento, dirección, temperatura, humedad y nubosidad son **instantáneos**.

Para la franja [h, h+1) cada variable se evalúa así:

- **Instantáneas** (viento, temperatura, humedad, nubosidad): deben cumplir en h y en h+1.
- **Ráfaga:** se usa el valor de la marca h+1.
- **Lluvia "en la hora y las 4 siguientes":** marcas h+1 a h+5.
- **Valor faltante:** la franja queda no apta, con causa SIN_DATOS.
- **Luz solar:** la franja tiene que caer completa entre la salida y la puesta del sol.

## Trazabilidad de reglas

| Regla | Dónde se implementa | Pruebas principales |
| --- | --- | --- |
| RN-01 licencia vigente el día del vuelo | `AsignadorRecursos.disponible` | `licenciaVencidaElDiaDelVueloAunqueHoyEsteVigente` |
| RN-02 habilitación y 20 h para pulverizar | `AsignadorRecursos.habilitado` | `pilotoSinHorasMinimasParaPulverizarNoSeAsigna`, `pilotoSinHabilitacionParaElTipoNoSeAsigna` |
| RN-03 sin solapamiento y tope de 6 h por día | `AsignadorRecursos.disponible`, `MisionRepository.minutosProgramados` | `unPilotoNoPuedeSuperarSeisHorasProgramadasEnElDia`, `unaMisionDeMasDeSeisHorasNoEncuentraPiloto` |
| RN-04 registro, seguro y estado OPERATIVO | `AsignadorRecursos.esApto` y `vigenteEl` | `dronConSeguroVencidoElDiaDelVueloNoEsApto` |
| RN-05 aptitud del dron | `AsignadorRecursos.esApto` | `sinDronAptoParaElTipoDeMision`, `monitoreoYSiembraUsanSusPropiasCondicionesDeEquipamiento` |
| RN-06 dron sin misiones solapadas | `MisionRepository.dronOcupado` + bloqueo | `elDronNoSePuedeUsarEnDosMisionesSolapadas`, `dosSolicitudesSimultaneasPorElUnicoDronNoQuedanAmbasProgramadas` |
| RN-07 solo el titular | `SolicitudMisionService.solicitar` | `loteDeOtroProductorRespondeComoSiNoExistiera` |
| RN-08 receta y producto | `SolicitudMisionRequest` (validación) | `pulverizacionSinRecetaEsEntradaInvalida`, `monitoreoConProductoEsEntradaInvalida` |
| RN-09 etapa prohibida | `Producto.prohibidoEn` | `etapaProhibidaParaElProductoSeRechazaSinConsultarElClima` |
| RN-10 intervalo mínimo (solo COMPLETADAS) | `MisionRepository.ultimaAplicacionCompletada` | `intervaloMinimoDesdeLaUltimaAplicacionCompletada`, `elIntervaloSoloMiraMisionesCompletadasNoProgramadas` |
| Umbrales por tipo y límite del modelo | `EvaluadorFranjas`, `RangoUmbral` | `EvaluadorFranjasTest`, `RangoUmbralTest` |
| RN-14 duración | `CalculadoraDuracion` | `CalculadoraDuracionTest` |
| RN-15 ventanas y mejor margen | `BuscadorVentanas` | `BuscadorVentanasTest`, `pulverizacionValidaQuedaProgramadaConVentanaPilotoYDron` |
| RN-16 sin ventana y variable más bloqueante | `ResultadoBusqueda.causaPrincipal` | `sinVentanaInformaLaCausaQueMasFranjasBloqueo`, `sinVentanaClimaticaSeInformaLaVariableQueMasBloqueo` |
| Falla cerrada (ms-campos y clima) | `CamposCliente`, `OpenMeteoProveedor` | `CamposClienteTest`, `OpenMeteoProveedorTest`, `siMsCamposNoRespondeFallaCerradoSinGuardarNada` |
| Transiciones de estado | `EstadoMision`, `Mision.transicionarA` | `cancelarLiberaElPilotoYElDron`, `unaMisionRechazadaNoSePuedeCancelar` |

## Límites conocidos

- RN-10 solo mira misiones COMPLETADAS, como pide el dominio. Dos pulverizaciones PROGRAMADAS del mismo producto sobre el mismo lote pueden quedar a menos del intervalo; como el MVP no ejecuta misiones, el caso no llega a materializarse.
- El estado administrativo del piloto y del dron no tiene endpoint de cambio en el MVP (D-19).
- El cliente `agrodrones-web` admite login directo con usuario y clave para Postman y pruebas.
