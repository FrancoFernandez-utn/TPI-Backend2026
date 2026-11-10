#!/bin/sh
# Crea un esquema y un usuario de base por microservicio. Cada usuario es dueno solo de su esquema:
# ms-campos no puede leer el esquema misiones y viceversa (separacion de datos entre servicios).
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
    CREATE USER campos_user WITH PASSWORD '${CAMPOS_DB_PASSWORD}';
    CREATE USER misiones_user WITH PASSWORD '${MISIONES_DB_PASSWORD}';

    CREATE SCHEMA campos AUTHORIZATION campos_user;
    CREATE SCHEMA misiones AUTHORIZATION misiones_user;

    REVOKE ALL ON SCHEMA public FROM PUBLIC;
    ALTER ROLE campos_user SET search_path = campos;
    ALTER ROLE misiones_user SET search_path = misiones;
SQL
