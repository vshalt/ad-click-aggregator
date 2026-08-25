# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 as builder
WORKDIR /app
COPY pom.xml .
COPY consumer-service/pom.xml consumer-service/
RUN mvn -f consumer-service/pom.xml dependency:go-offline
COPY consumer-service/src consumer-service/src
RUN mvn -f consumer-service/pom.xml package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:25-jdk-ubi10-minimal
WORKDIR /app
COPY --from=builder /app/consumer-service/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]