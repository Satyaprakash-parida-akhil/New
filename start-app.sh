#!/bin/sh

# Log the initial URL (masking password)
# FORCE use of internal pieces and IGNORE any external URL
echo "Bypassing external Render URL and forcing internal connection..."

# Diagnostic: Check if internal variables are present
[ -z "$DB_HOST" ] && echo "ERROR: DB_HOST is empty"
[ -z "$DB_PORT" ] && echo "ERROR: DB_PORT is empty"
[ -z "$DB_NAME" ] && echo "ERROR: DB_NAME is empty"

FIXED_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?user=${DB_USER}&password=${DB_PASS}&sslmode=require&connectTimeout=10"

# Masked log for safety
echo "Forcing JDBC URL to Internal Host: ${DB_HOST} (Port: ${DB_PORT})"

# Run the app with explicit credentials to leave no doubt
exec java \
  -jar app.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url="$FIXED_URL" \
  --spring.datasource.username="$DB_USER" \
  --spring.datasource.password="$DB_PASS"
