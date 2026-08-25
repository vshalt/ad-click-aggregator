# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 as builder
WORKDIR /app
COPY pom.xml .
COPY ingestion-service/pom.xml ingestion-service/
RUN mvn -f ingestion-service/pom.xml dependency:go-offline
COPY ingestion-service/src ingestion-service/src
RUN mvn -f ingestion-service/pom.xml package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:25-jdk-ubi10-minimal
WORKDIR /app
COPY --from=builder /app/ingestion-service/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]