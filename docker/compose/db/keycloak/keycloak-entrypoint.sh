#!/bin/sh

echo ">> Starting import of realms..."
/opt/keycloak/bin/kc.sh import --dir=/opt/keycloak/data/import --override=true --verbose

echo ">> Starting Keycloak..."
exec /opt/keycloak/bin/kc.sh start-dev
