#!/bin/bash
set -e

# Start the PostgreSQL server in the background
docker-entrypoint.sh postgres -c timezone=UTC &

# Wait for PostgreSQL to be ready
until pg_isready -U postgres; do
    echo 'Waiting for PostgreSQL to be ready...'
    sleep 5
done

echo 'Executing db creation...'
# Run the init SQL script
psql -U postgres -d postgres -f /init_db.sql || true

wait
