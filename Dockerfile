FROM node:24-alpine AS frontend-build

WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21-alpine AS backend-build

WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn --batch-mode dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend-build --chown=app:app /workspace/backend/target/backend-*.jar app.jar

USER app
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=15s --retries=12 CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
