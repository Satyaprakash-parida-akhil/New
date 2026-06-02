#!/bin/sh

# Log the initial URL (masking password)
MASKED_URL=$(echo $SPRING_DATASOURCE_URL | sed 's/:[^:@]*@/:****@/')
echo "Starting application with Source URL: $MASKED_URL"

# Build the perfect JDBC URL from individual pieces
FIXED_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?user=${DB_USER}&password=${DB_PASS}&sslmode=require"

# Masked log for safety
echo "Built JDBC URL for Host: ${DB_HOST}"

# Run the app
exec java -jar app.jar --spring.profiles.active=prod --spring.datasource.url="$FIXED_URL"
