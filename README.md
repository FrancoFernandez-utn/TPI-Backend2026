# AgroDrones: operaciones con drones en el campo

TPI de Backend de Aplicaciones 2026 (UTN FRC), Dominio 2.

Un productor solicita una misión de dron sobre uno de sus lotes. El sistema decide si se puede hacer, en qué ventana horaria y con qué piloto y dron:

- **Si se puede:** la misión queda PROGRAMADA, con ventana, piloto y dron asignados.
- **Si no se puede:** la misión queda RECHAZADA, con la lista de motivos.

La decisión combina tres fuentes:

- **El terreno:** `ms-campos`.
- **La capacidad operativa:** pilotos y drones en `ms-misiones`.
- **El pronóstico horario:** Open-Meteo.

El alcance implementado es el **MVP de la sección 17** del documento de dominio. El detalle de las decisiones está en [`docs/decisiones.md`](docs/decisiones.md) y los diagramas en [`docs/diagramas.md`](docs/diagramas.md).

## Arquitectura

| Componente | Responsabilidad | Puerto |
| --- | --- | --- |
| `api-gateway` | Punto único de entrada. Valida el JWT, enruta `/api/**` y bloquea `/internal/**`. Publica Swagger agregado | **8080** (publicado) |
| `ms-campos` | Establecimientos, lotes, historial de cultivo y etapa, titularidad. Informa el contexto del lote | 8082 (interno) |
| `ms-misiones` | Pilotos, drones, productos, umbrales y misiones. Decide la autorización y es el único que consulta el clima | 8083 (interno) |
| `agrodrones-commons` | JAR común: formato de error, excepciones, seguridad Keycloak y OpenAPI | — |
| Keycloak 26.8 | Identity Provider (OIDC, JWT, roles) con el realm `agrodrones` | **8081** (publicado) |
| PostgreSQL 16 | Esquemas `campos` y `misiones`, cada uno con su propio usuario de base | interno |

Solo el Gateway y Keycloak publican puertos. Los microservicios y la base quedan en la red interna de Compose.

## Requisitos

| Para | Necesitás |
| --- | --- |
| Levantar el sistema | Docker Desktop (o Docker Engine con Compose v2) |
| Compilar y correr las pruebas fuera de Docker | JDK 21 y Maven 3.9. Las pruebas usan un PostgreSQL embebido: **no necesitan Docker** |
| Ejecutar la colección | Postman, o Newman (`npm install -g newman`) |

## Levantar el sistema

```bash
docker compose up -d --build
```

La primera vez tarda varios minutos, porque compila las imágenes. Cuando `docker compose ps` muestra todos los servicios como `healthy`, el sistema está listo:

| Qué | URL |
| --- | --- |
| API (Gateway) | http://localhost:8080 |
| Swagger UI (ambos servicios) | http://localhost:8080/swagger-ui.html |
| Keycloak (consola de administración: `admin` / `admin`) | http://localhost:8081 |

### Clima real o simulado

`ms-misiones` puede usar el pronóstico real de Open-Meteo (por defecto) o un proveedor simulado que no necesita Internet:

```bash
# Pronóstico real (requiere Internet)
docker compose up -d --build

# Escenarios fijos y reproducibles para la demo
CLIMA_PROVEEDOR=simulado docker compose up -d --build
```

En PowerShell: `$env:CLIMA_PROVEEDOR="simulado"; docker compose up -d --build`. También se puede copiar `.env.example` como `.env` y editarlo.

En modo simulado, el clima de cada lote depende de sus coordenadas:

| Coordenadas del lote | Escenario |
| --- | --- |
| -33.1234, -64.3456 | VENTOSO: no hay ventana para ningún tipo de misión |
| -32.5, -63.5 | SIN_SERVICIO: la fuente climática no responde (503) |
| cualquier otra | BUENO: mañanas tranquilas, tardes algo más ventosas |

### Usuarios de prueba

| Usuario | Clave | Rol |
| --- | --- | --- |
| `admin1` | `admin123` | ADMIN |
| `productor1` | `productor123` | PRODUCTOR |
| `productor2` | `productor123` | PRODUCTOR |
| `piloto1` | `piloto123` | PILOTO |
| `piloto2` | `piloto123` | PILOTO |

El cliente `agrodrones-web` es público y admite login directo con usuario y clave. Sirve para Postman y para pruebas, no para producción:

```bash
curl -d grant_type=password -d client_id=agrodrones-web \
     -d username=productor1 -d password=productor123 \
     http://localhost:8081/realms/agrodrones/protocol/openid-connect/token
```

### Datos de demostración

Al arrancar se cargan:

- un **Lote Histórico** (id `900001`) de `productor1`;
- una **misión COMPLETADA** hace 2 días con el producto **Herbicida Histórico** (id `900001`, intervalo de 7 días).

Con eso se demuestra RN-10 sin implementar el ciclo de ejecución (decisión D-02). Para no cargarlos, usá `FLYWAY_LOCATIONS=classpath:db/migration`.

## Colección Postman

Está en `postman/`. Para usarla en Postman, importá `agrodrones.postman_collection.json` y el entorno `agrodrones-local.postman_environment.json`, y ejecutá las carpetas en orden. Con Newman:

```bash
newman run postman/agrodrones.postman_collection.json -e postman/agrodrones-local.postman_environment.json
```

Recorre el proceso completo con aserciones:

- autenticación;
- seguridad (401, 403, `/internal` bloqueado);
- alta del campo, catálogos, pilotos y drones;
- misión PROGRAMADA y consulta de ventanas;
- rechazos por RN-09, RN-10 y RN-16;
- validaciones 400 y 404;
- cancelación (200 y 409);
- falla cerrada (503).

Se puede ejecutar varias veces seguidas. Los dos pasos que dependen del clima simulado se omiten solos si el sistema usa Open-Meteo real.

## Compilar y probar sin Docker

`agrodrones-commons` es un proyecto Maven independiente. Se instala primero en el repositorio local (`.m2`) y los servicios lo consumen como dependencia:

```bash
mvn -f agrodrones-commons/pom.xml clean install
mvn -f ms-campos/pom.xml clean verify
mvn -f ms-misiones/pom.xml clean verify
mvn -f api-gateway/pom.xml clean verify
```

Las pruebas de integración levantan un **PostgreSQL 16 real embebido**, con las migraciones Flyway reales, y no necesitan Docker. Ese PostgreSQL no arranca si Maven se ejecuta como usuario `root`.

| Proyecto | Pruebas | Qué cubren |
| --- | --- | --- |
| agrodrones-commons | 16 | Formato de error, manejador global, conversor de roles, usuario actual |
| ms-campos | 11 | Alta y propiedad de lotes, historial de etapas, contexto interno, seguridad |
| ms-misiones | 78 | Motor climático, reglas RN-01 a RN-16, asignación, concurrencia sobre PostgreSQL, cancelación, Open-Meteo con dobles, seguridad |
| api-gateway | 7 | Enrutamiento, validación de token, reenvío del token, bloqueo de `/internal` |

## Variables de entorno

Todas tienen valor por defecto en `docker-compose.yml`. Ver también `.env.example`.

| Variable | Servicio | Uso |
| --- | --- | --- |
| `CLIMA_PROVEEDOR` | ms-misiones | `openmeteo` (real) o `simulado` |
| `FLYWAY_LOCATIONS` | ms-campos, ms-misiones | Incluye o no los datos de demostración |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | postgres | Superusuario de la base |
| `CAMPOS_DB_PASSWORD`, `MISIONES_DB_PASSWORD` | postgres y servicios | Clave del usuario de base de cada servicio |
| `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` | keycloak | Administrador de la consola |
| `MS_MISIONES_CLIENT_SECRET` | ms-misiones | Secreto del cliente de servicio (debe coincidir con el realm) |

Las variables internas que fija Compose son:

- `DB_URL`, `DB_USER`, `DB_PASSWORD`;
- `KEYCLOAK_JWK_SET_URI`, `KEYCLOAK_ISSUER`, `KEYCLOAK_TOKEN_URI`;
- `MS_CAMPOS_URL`, `MS_MISIONES_URL`.

**Emisor del token.** Keycloak usa una URL pública fija (`KC_HOSTNAME=http://localhost:8081`). Por eso todos los tokens llevan ese emisor, aunque los servicios descarguen las claves y pidan tokens por la red interna (`http://keycloak:8080`). Es lo que evita el error clásico de "emisor no coincide" en Docker.

## Endpoints principales

| Método y ruta | Rol |
| --- | --- |
| `POST /api/establecimientos`, `POST /api/establecimientos/{id}/lotes`, `PUT /api/lotes/{id}/cultivo`, `GET /api/lotes/mios` | PRODUCTOR |
| `POST /api/pilotos`, `GET /api/pilotos/yo` | PILOTO |
| `POST /api/drones` | ADMIN, PILOTO |
| `POST /api/modelos-dron`, `POST /api/productos`, `PUT /api/umbrales/{tipo}` | ADMIN |
| `GET /api/modelos-dron`, `GET /api/productos`, `GET /api/umbrales` | autenticado |
| `POST /api/misiones`, `POST /api/misiones/{id}/cancelar` | PRODUCTOR |
| `GET /api/misiones/{id}`, `GET /api/misiones/mias` | PRODUCTOR (las suyas), PILOTO (asignadas), ADMIN |
| `GET /api/misiones/{id}/ventanas` | PRODUCTOR, ADMIN |
| `GET /internal/lotes/{id}/contexto` | SERVICIO (solo red interna) |

Respuestas de error, todas con el mismo formato (`codigo`, `mensaje`, `detalles`):

| Código | Cuándo |
| --- | --- |
| 400 | Entrada inválida |
| 401 | Sin token o token inválido |
| 403 | Rol insuficiente |
| 404 | Recurso inexistente o ajeno |
| 409 | Transición o duplicado |
| 503 | Dependencia caída, con `Retry-After` |

Un rechazo por regla de negocio no es un error: `POST /api/misiones` responde 201 con la misión RECHAZADA y sus motivos.

## Estructura del repositorio

```
agrodrones-commons/   JAR común (proyecto Maven independiente)
api-gateway/          Spring Cloud Gateway
ms-campos/            terreno y titularidad
ms-misiones/          recursos, clima y decisión
infra/                realm de Keycloak e inicialización de PostgreSQL
postman/              colección y entorno
docs/                 decisiones y diagramas
docker-compose.yml
```
