# Stage 1: Build Angular Frontend
FROM node:22 AS frontend-build
WORKDIR /app
COPY dms-frontend/package*.json ./
RUN npm install --legacy-peer-deps
COPY dms-frontend/ ./
RUN NODE_OPTIONS="--max-old-space-size=400" npm run build -- --configuration=production

# Stage 2: Build Spring Boot Backend with Integrated Frontend
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY dms/pom.xml .
COPY dms/src ./src
RUN mkdir -p src/main/resources/static
COPY --from=frontend-build /app/dist/smart-bazar/browser/ src/main/resources/static/
RUN MAVEN_OPTS="-Xmx256m" mvn clean package -DskipTests

# Stage 3: Final Production Image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar

EXPOSE 8080

# G1GC is more efficient at managing memory in fragmented, small containers
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-Xmx200m", "-Xms128m", "-jar", "app.jar", "--spring.profiles.active=prod"]
