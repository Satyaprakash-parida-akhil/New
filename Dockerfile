# Stage 1: Build Angular Frontend
FROM node:22 AS frontend-build
WORKDIR /app
COPY dms-frontend/package*.json ./
RUN npm install --legacy-peer-deps
COPY dms-frontend/ ./
# Inject the Production URL into the frontend build
ARG API_URL=https://api.example.com/api
RUN sed -i "s|https://api.example.com/api|$API_URL|g" src/environments/environment.prod.ts
RUN npm run build -- --configuration=production

# Stage 2: Build Spring Boot Backend with Integrated Frontend
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY dms/pom.xml .
COPY dms/src ./src
# Create the static resources folder if it doesn't exist
RUN mkdir -p src/main/resources/static
# Copy the built Angular app into the Spring Boot static folder
COPY --from=frontend-build /app/dist/dms-frontend/browser/ src/main/resources/static/
RUN mvn clean package -DskipTests

# Stage 3: Final Production Image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar

# Port 8080 is the standard for Railway
EXPOSE 8080

# Run with Production Profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
