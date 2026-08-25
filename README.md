# Ad Click Aggregator

Multi-module Spring Boot project for ad click aggregation with Kafka and PostgreSQL.

## Project Structure

```
ad-click-aggregator/
├── ingestion-service/     # Ingestion service (port 8080)
├── consumer-service/      # Consumer service (port 8081)
├── docker-compose.yml     # Docker orchestration
└── pom.xml               # Parent POM
```

## Services

- **Ingestion Service**: Runs on port 8080, produces messages to Kafka
- **Consumer Service**: Runs on port 8081, consumes from Kafka and writes to PostgreSQL
- **PostgreSQL**: Database on port 5432
- **Kafka**: Message broker on port 9092

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 25
- Maven 3.9+

### Running the Application

1. **Start all services** (infrastructure + both Spring Boot apps):
   ```bash
   docker-compose up --build
   ```

2. **Start only infrastructure** (if you want to run Spring Boot apps locally):
   ```bash
   docker-compose up postgres kafka
   ```

3. **Build the project locally**:
   ```bash
   mvn clean install
   ```

4. **Run individual services locally** (from project root):
   ```bash
   # Terminal 1 - Ingestion Service
   cd ingestion-service && mvn spring-boot:run
   
   # Terminal 2 - Consumer Service
   cd consumer-service && mvn spring-boot:run
   ```

### Accessing Services

- Ingestion Service: http://localhost:8080
- Consumer Service: http://localhost:8081
- PostgreSQL: localhost:5432
- Kafka: localhost:9092

### Stopping Services

```bash
docker-compose down
```

To remove volumes as well:
```bash
docker-compose down -v
```

## Development Workflow

1. Code changes are made locally in your IDE
2. Both services run in Docker containers
3. Rebuild containers when needed: `docker-compose up --build`
4. View logs: `docker-compose logs -f [service-name]`

## Environment Variables

Both services support environment variable overrides:

- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (default: localhost:9092)
- `SPRING_DATASOURCE_URL` (default: jdbc:postgresql://localhost:5432/postgres)
- `SPRING_DATASOURCE_USERNAME` (default: postgres)
- `SPRING_DATASOURCE_PASSWORD` (default: postgres)
