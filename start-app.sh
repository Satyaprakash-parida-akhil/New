#!/bin/sh

# Log the initial URL (masking password)
MASKED_URL=$(echo $SPRING_DATASOURCE_URL | sed 's/:[^:@]*@/:****@/')
echo "Starting application with Source URL: $MASKED_URL"

# Robust conversion from postgres(ql):// to jdbc:postgresql://
FIXED_URL=$(echo $SPRING_DATASOURCE_URL | sed 's/^postgresql:/jdbc:postgresql:/' | sed 's/^postgres:/jdbc:postgresql:/')

# Add SSL mode if missing (required for Render/Supabase)
if ! echo "$FIXED_URL" | grep -q "sslmode="; then
    if echo "$FIXED_URL" | grep -q "?"; then
        FIXED_URL="${FIXED_URL}&sslmode=require"
    else
        FIXED_URL="${FIXED_URL}?sslmode=require"
    fi
fi

# Log the final URL (masking password)
MASKED_FIXED=$(echo $FIXED_URL | sed 's/:[^:@]*@/:****@/')
echo "Fixed JDBC URL: $MASKED_FIXED"

# Run the app
exec java -jar app.jar --spring.profiles.active=prod --spring.datasource.url="$FIXED_URL"
