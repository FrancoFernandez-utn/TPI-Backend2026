# Diagramas

Los diagramas representan la arquitectura implementada y desplegada con `docker-compose.yml`. Están escritos en Mermaid, que GitHub muestra como imagen.

## C1: contexto

```mermaid
flowchart LR
    productor["Productor<br/>solicita misiones sobre sus lotes"]
    piloto["Piloto<br/>registra su perfil y sus drones"]
    admin["Administrador<br/>catálogos y umbrales"]
    sistema["Plataforma AgroDrones<br/>decide si una misión se autoriza,<br/>cuándo y con quién"]
    keycloak["Keycloak<br/>identidad: OIDC, JWT, roles"]
    meteo["Open-Meteo<br/>pronóstico horario y luz solar"]

    productor -- "HTTPS + JWT" --> sistema
    piloto -- "HTTPS + JWT" --> sistema
    admin -- "HTTPS + JWT" --> sistema
    productor -. "login" .-> keycloak
    piloto -. "login" .-> keycloak
    admin -. "login" .-> keycloak
    sistema -- "valida tokens" --> keycloak
    sistema -- "consulta el pronóstico" --> meteo
```

## C2: contenedores

```mermaid
flowchart TB
    usuarios["Usuarios<br/>Productor, Piloto, Admin"]
    keycloak["Keycloak 26<br/>realm agrodrones<br/>:8081"]
    meteo["Open-Meteo<br/>(externo)"]

    subgraph interna["Red interna de Docker Compose (sin puertos publicados)"]
        campos["ms-campos<br/>Spring Boot<br/>establecimientos, lotes, cultivos"]
        misiones["ms-misiones<br/>Spring Boot<br/>pilotos, drones, misiones,<br/>decisión y ventanas"]
        pg[("PostgreSQL 16<br/>esquemas campos y misiones")]
    end

    gateway["API Gateway<br/>Spring Cloud Gateway<br/>:8080"]
    commons["agrodrones-commons<br/>JAR Maven"]

    usuarios -- "HTTPS + JWT" --> gateway
    usuarios -. "login OIDC" .-> keycloak
    gateway -- "valida JWT (JWKS)" --> keycloak
    gateway -- "/api/establecimientos, /api/lotes" --> campos
    gateway -- "/api/misiones, pilotos, drones, catálogos" --> misiones
    misiones -- "GET /internal/lotes/{id}/contexto<br/>token client credentials (SERVICIO)" --> campos
    misiones -- "token de servicio" --> keycloak
    misiones -- "pronóstico (timeout, reintentos,<br/>circuit breaker)" --> meteo
    campos -- "JDBC (campos_user)" --> pg
    misiones -- "JDBC (misiones_user)" --> pg
    commons -. "dependencia Maven" .-> campos
    commons -. "dependencia Maven" .-> misiones
```

## Secuencia: solicitud de una misión

```mermaid
sequenceDiagram
    autonumber
    actor P as Productor
    participant G as API Gateway
    participant M as ms-misiones
    participant K as Keycloak
    participant C as ms-campos
    participant O as Open-Meteo
    participant DB as PostgreSQL (misiones)

    P->>G: POST /api/misiones (JWT)
    G->>G: valida firma, vencimiento y emisor
    G->>M: reenvía con el JWT del usuario
    M->>M: valida JWT, rol PRODUCTOR y entrada (RN-08)
    M->>K: token de servicio (client credentials, cacheado)
    M->>C: GET /internal/lotes/{id}/contexto
    alt ms-campos no responde
        M-->>P: 503 + Retry-After (falla cerrada, no se guarda nada)
    end
    C-->>M: titular, coordenadas, hectáreas, cultivo, etapa
    M->>M: RN-07 titular (ajeno = 404), RN-09 etapa, RN-10 intervalo
    alt rechazo por reglas del lote o del producto
        M->>DB: misión RECHAZADA con motivos
        M-->>P: 201 RECHAZADA
    end
    M->>O: pronóstico horario + salida y puesta del sol
    alt Open-Meteo no responde
        M-->>P: 503 (falla cerrada)
    end
    M->>M: por modelo de dron: duración (RN-14), franjas aptas, ventanas (RN-15)
    M->>DB: BEGIN - SELECT drones y pilotos FOR UPDATE (orden por id)
    M->>DB: verifica RN-01 a RN-06 en cada ventana, de mejor a peor
    alt hay ventana, dron y piloto
        M->>DB: misión PROGRAMADA + evaluación APTA (COMMIT)
        M-->>P: 201 PROGRAMADA (ventana, piloto, dron)
    else sin ventana o sin recursos
        M->>DB: misión RECHAZADA + evaluación (COMMIT)
        M-->>P: 201 RECHAZADA (SIN_VENTANA_CLIMATICA y variable, o sin recursos)
    end
```

## Estados de la misión

```mermaid
stateDiagram-v2
    [*] --> SOLICITADA
    SOLICITADA --> RECHAZADA: algún motivo de rechazo
    SOLICITADA --> AUTORIZADA: hay ventana, dron y piloto
    AUTORIZADA --> PROGRAMADA: reserva confirmada (misma transacción)
    AUTORIZADA --> CANCELADA: el productor cancela
    PROGRAMADA --> CANCELADA: el productor cancela
    RECHAZADA --> [*]
    CANCELADA --> [*]
    note right of PROGRAMADA
        COMPLETADA existe solo en datos
        históricos (RN-10, decisión D-02)
    end note
```

## Modelo de datos

```mermaid
erDiagram
    ESTABLECIMIENTO ||--o{ LOTE : tiene
    LOTE ||--o{ CULTIVO_LOTE : "historial de etapas"
    ESTABLECIMIENTO {
        bigint id PK
        varchar productor_sub "sub de Keycloak"
        varchar nombre
    }
    LOTE {
        bigint id PK
        bigint establecimiento_id FK
        numeric lat
        numeric lon
        numeric hectareas
        varchar estado
    }
    CULTIVO_LOTE {
        bigint id PK
        bigint lote_id FK
        varchar cultivo
        varchar etapa
        timestamptz desde
    }

    PILOTO ||--o{ HABILITACION : tiene
    MODELO_DRON ||--o{ DRON : "es de"
    PRODUCTO ||--o{ PRODUCTO_ETAPA_PROHIBIDA : prohibe
    PILOTO ||--o{ MISION : "asignado a"
    DRON ||--o{ MISION : "asignado a"
    PRODUCTO ||--o{ MISION : aplica
    MISION ||--o{ EVALUACION_MISION : "justificada por"
    PILOTO {
        bigint id PK
        varchar usuario_sub UK
        date licencia_vence
        numeric horas_vuelo
        varchar estado
    }
    DRON {
        bigint id PK
        bigint modelo_id FK
        varchar matricula UK
        date registro_vence
        date seguro_vence
        varchar estado
    }
    MODELO_DRON {
        bigint id PK
        numeric capacidad_l
        numeric ha_por_hora
        numeric ha_por_vuelo
        numeric limite_rafaga_kmh
        boolean tiene_camara
    }
    PRODUCTO {
        bigint id PK
        varchar nombre UK
        int intervalo_dias
    }
    MISION {
        bigint id PK
        bigint lote_id "referencia lógica a ms-campos"
        varchar productor_sub
        varchar tipo
        varchar estado
        timestamptz inicio_prev
        timestamptz fin_prev
        jsonb contexto_json "snapshot del lote"
        jsonb motivos_json
    }
    EVALUACION_MISION {
        bigint id PK
        bigint mision_id FK
        varchar resultado
        jsonb variables_json
        jsonb ventanas_json
    }
    UMBRAL_CLIMA {
        varchar tipo_mision PK
        varchar variable PK
        numeric minimo
        numeric maximo
    }
```

Los esquemas `campos` y `misiones` no tienen claves foráneas entre sí: `mision.lote_id` y `mision.productor_sub` son referencias lógicas a datos de `ms-campos`.
