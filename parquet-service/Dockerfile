# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 as builder
ARG MAVEN_EXTRA_ARGS=""
WORKDIR /app
COPY pom.xml .
COPY parquet-service/pom.xml parquet-service/
RUN mvn -f parquet-service/pom.xml dependency:go-offline $MAVEN_EXTRA_ARGS
COPY parquet-service/src parquet-service/src
RUN mvn -f parquet-service/pom.xml package -DskipTests $MAVEN_EXTRA_ARGS

# Stage 2: Run
FROM eclipse-temurin:25-jdk-ubi10-minimal
WORKDIR /app
COPY --from=builder /app/parquet-service/target/*.jar /app/app.jar
ENV PARQUET_OUTPUT_DIR=/data/parquet
VOLUME /data/parquet
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/app.jar"]
