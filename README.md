# Ingestion API Microservice

## Overview

This microservice implements a **Scalable Offer Ingestion System (SOIS)** designed to handle massive data volumes (170,000+ records) using event-driven architecture, reactive stream processing, and polyglot persistence.

## Architecture

The project follows **Hexagonal Architecture (Ports & Adapters)** with clear separation of concerns:

```
ingestion-api/
├── ingestion-domain/              # Core business logic (entities, value objects, ports)
├── ingestion-application/         # Use cases, application services, orchestration
├── ingestion-boot/                # Spring Boot application entry point
├── ingestion-infra-input-rest/    # REST API adapters
├── ingestion-infra-input-scheduler/ # Scheduled file processors
├── ingestion-infra-output-kafka/  # Kafka messaging adapter
├── ingestion-infra-output-mongodb/ # MongoDB persistence adapter
├── ingestion-infra-output-postgresql/ # PostgreSQL persistence adapter
└── ingestion-factory-data-test/   # Test data factories
```

## Technology Stack

- **Java 21** (Virtual Threads / Project Loom)
- **Spring Boot 3.4.1**
- **Spring Batch 5.1.2** (Chunk-oriented processing)
- **Apache Kafka** (Event streaming)
- **MongoDB** (Document store - source of truth)
- **PostgreSQL 17** (Relational store - search index)
- **StAX + Jackson** (Memory-efficient XML/JSON streaming)

## Key Features

### Ingestion Methods

1. **REST JSON Batch**: Standard endpoint for small-to-medium datasets
2. **REST InputStream**: Large file processing via streaming
3. **URL Pull**: Remote file download and processing
4. **Scheduled Watcher**: Automatic file system/SFTP monitoring

### Scalability Features

- **Constant Memory Footprint**: Process 2GB files with ~256MB RAM
- **Horizontal Scalability**: Kafka partitions enable multiple worker instances
- **Fault Isolation**: Kafka buffers events during downstream failures
- **Asynchronous Decoupling**: Non-blocking ingestion pipeline

## Prerequisites

- Java 21
- Docker & Docker Compose (for local infrastructure)
- Gradle 8.x

## Quick Start

### 1. Start Local Infrastructure

```bash
docker-compose up -d
```

This starts:
- MongoDB (port 27017)
- PostgreSQL (port 5432)
- Apache Kafka (port 9092)
- Zookeeper (port 2181)

### 2. Build the Project

```bash
./gradlew clean build
```

### 3. Run the Application

```bash
./gradlew :ingestion-boot:bootRun
```

The application will start on port 8080.

## Module Descriptions

### ingestion-domain
Pure business logic with no external dependencies. Contains:
- Domain entities (Offer, Vehicle, etc.)
- Value objects
- Port interfaces (repositories, messaging)
- Domain events

### ingestion-application
Application services and use cases:
- Ingestion orchestration
- Batch processing coordination
- Business rules validation
- Transaction management

### ingestion-boot
Spring Boot configuration and main application class:
- Application configuration
- Dependency injection wiring
- Profile management
- Bootstrap logic

### ingestion-infra-input-rest
REST API controllers and DTOs:
- Batch ingestion endpoints
- Stream upload endpoints
- URL trigger endpoints
- API documentation (OpenAPI/Swagger)

### ingestion-infra-input-scheduler
Scheduled tasks and watchers:
- File system monitoring
- SFTP integration
- Cron-based triggers
- Spring Batch job launching

### ingestion-infra-output-kafka
Kafka producers and consumers:
- Event publishing
- Message serialization
- Topic configuration
- Error handling

### ingestion-infra-output-mongodb
MongoDB repositories and entities:
- Document persistence
- Full offer details storage
- Indexing strategy
- Query optimization

### ingestion-infra-output-postgresql
PostgreSQL repositories and JPA entities:
- Relational persistence
- Search index maintenance
- Batch job metadata
- Flyway migrations

### ingestion-factory-data-test
Test data builders and factories for unit/integration tests.

## Building and Testing

### Run All Tests
```bash
./gradlew test
```

### Run Tests for Specific Module
```bash
./gradlew :ingestion-domain:test
```

### Generate Test Coverage Report
```bash
./gradlew testCodeCoverageReport
```

Report available at: `build/reports/jacoco/testCodeCoverageReport/html/index.html`

### Code Quality (Spotless)
```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

### SonarQube Analysis
```bash
./gradlew sonar -Dsonar.host.url=<your-sonar-url> -Dsonar.login=<token>
```

## Configuration

Key configuration in `ingestion-boot/src/main/resources/application.yml`:

- **Batch chunk size**: `ingestion.batch.chunk-size`
- **Kafka topics**: `ingestion.kafka.topics.offers`
- **Scheduler cron**: `ingestion.scheduler.cron`
- **Watch directory**: `ingestion.scheduler.watch-directory`

## API Documentation

Once running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## Monitoring

Prometheus metrics available at:
```
http://localhost:8080/actuator/prometheus
```

Health check:
```
http://localhost:8080/actuator/health
```


