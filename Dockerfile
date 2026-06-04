# Stage 1: Build Angular Frontend
FROM node:22 AS frontend-build
WORKDIR /app
COPY dms-frontend/package*.json ./
RUN npm install --legacy-peer-deps
COPY dms-frontend/ ./
RUN npm run build -- --configuration=production

# Stage 2: Build Spring Boot Backend with Integrated Frontend
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY dms/pom.xml .
COPY dms/src ./src
RUN mkdir -p src/main/resources/static
COPY --from=frontend-build /app/dist/dms-frontend/browser/ src/main/resources/static/
RUN mvn clean package -DskipTests

# Stage 3: Final Production Image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar

EXPOSE 8080

# RENDER_DB_URL is NOT a Spring magic name, so it won't auto-override.
# We convert postgresql:// -> jdbc:postgresql:// and pass it as the ONLY datasource.url
ENTRYPOINT ["sh", "-c", "JDBC=$(echo $RENDER_DB_URL | sed 's|^postgresql://|jdbc:postgresql://|;s|^postgres://|jdbc:postgresql://|') && echo \"JDBC URL: ${JDBC%%@*}@***\" && exec java -Xmx256m -Xms64m -jar app.jar --spring.profiles.active=prod --spring.datasource.url=$JDBC --spring.datasource.username=$RENDER_DB_USER --spring.datasource.password=$RENDER_DB_PASS"]
