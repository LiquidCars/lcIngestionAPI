#!/bin/bash
set -e

# Start the PostgreSQL server in the background
# Al iniciar con &, debemos capturar su PID para el 'wait' final
docker-entrypoint.sh postgres &
PG_PID=$!

# Wait for PostgreSQL service to be ready
until pg_isready -U postgres; do
    echo 'Waiting for PostgreSQL service to be ready...'
    sleep 2 # Reducido el sleep para ser más rápido
done

echo 'PostgreSQL is ready. Starting dump restoration...'

# Ejecutar DROP SCHEMA para limpiar la DB inicial
psql -U postgres -d keycloak -f /init_after_dump.sql || true

# Importar el dump. Este comando debe ser BLOQUEANTE.
# Nota: pg_restore con -U keycloak funciona porque el entrypoint de postgres:17.2
#       ya crea la base de datos 'keycloak' y el usuario 'keycloak' basado en las variables de entorno.
pg_restore -U keycloak -d keycloak /keycloak_dump.dmp

if [ $? -ne 0 ]; then
    echo "ERROR: pg_restore failed."
    # Si falla, podemos optar por detener el contenedor
    # kill $PG_PID
    # exit 1
    # Dejamos que continúe si prefieres (como tu || true anterior), pero es mejor que falle para un entorno de prueba
fi

echo 'Dump restoration complete. Database is fully populated.'

# Keep the main PostgreSQL process running (wait for the process we started)
wait $PG_PID
