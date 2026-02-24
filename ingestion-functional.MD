# Functional Specification: Offer Ingestion System (Updated)

## 1. Executive Summary

This document describes the architecture and functional requirements of the vehicle offer ingestion system (`ingestion-api`). The system is designed to process large volumes of data (170,000+ records) asynchronously, in a decoupled manner, and without impacting the availability of the main system (`lcAPI`).

The solution combines an **independent ingestion API** (`ingestion-api`, Spring Boot) with an **orchestrating monolith** (`lcAPI`) that manages the complete lifecycle through a **state machine (Workflow)**, with both systems communicating via **Apache Kafka**.

---

## 2. Business Objectives

### 2.1 Primary Goals

- **Eliminate downtime**: bulk imports without restarting the system or opening maintenance windows.
- **Scale to volume**: process from hundreds to hundreds of thousands of records efficiently.
- **Guarantee data integrity**: lifecycle controlled with auditable states and fault recovery.
- **Operational visibility**: real-time monitoring of every ingestion's status.

### 2.2 KPIs

| Indicator | Target |
|-----------|--------|
| Throughput | 170,000+ offers without degradation |
| Memory | Constant (~256 MB) regardless of file size |
| Fault tolerance | Continued processing despite individual record errors |
| Recovery | Resume from last checkpoint without duplicates |

---

## 3. System Architecture Overview

The system is composed of two main applications and a messaging layer:

```mermaid
graph TD
    subgraph "Entry Points (ingestion-api)"
        A1[REST API - JSON/XML]
        A2[Stream Upload - Large File]
        A3[Remote URL - Pull]
        A4[Directory Monitor - Automated]
    end

    subgraph "ingestion-api (Spring Boot)"
        B1[OfferIngestionProcessServiceImpl]
        B2[Spring Batch Job]
        B3[OfferStreamItemReader]
        B4[OfferItemWriter]
        B5[IngestionBatchConfig]
    end

    subgraph "Storage"
        C1[(PostgreSQL - Reports & Batch)]
        C2[(MongoDB - Draft Offers)]
        C3[(MongoDB - Final Offers)]
    end

    subgraph "Messaging (Kafka)"
        K1[Topic: Offers]
        K2[Topic: IngestionJobReport]
        K3[Topic: PromoteAction]
        K4[Topic: DeleteAction]
    end

    subgraph "lcAPI (Monolith)"
        D1[IngestionMicroService]
        D2[MicroIngestionWorkflowDispatcher]
        D3[WorkflowService - State Machine]
    end

    A1 & A2 & A3 & A4 --> B1
    B1 --> B2
    B2 --> B3 --> B4
    B4 --> C2
    B4 --> K1
    B1 --> C1
    B2 --> C1
    B1 --> K2

    K2 --> D1
    D1 --> D2
    D2 --> D3

    D2 --> K3
    D2 --> K4

    K3 --> B1
    K4 --> B1

    B1 --> C2
    B1 --> K2
```

---

## 4. Ingestion Modes

### 4.1 REST API (Small to Medium Batches)

**Use case**: Internal systems or partners submitting structured data via API.

**Characteristics**:
- Accepts JSON/XML, typically up to 1,000 records.
- Immediate payload validation.
- Synchronous processing via `processOffers()`, sending each offer to Kafka.
- Immediate acknowledgment with a report identifier.

**Flow**:
```mermaid
sequenceDiagram
    participant Client
    participant IngestionAPI
    participant Kafka
    participant lcAPI

    Client->>IngestionAPI: POST /offers (JSON/XML)
    IngestionAPI->>IngestionAPI: validateIngestionPayload()
    IngestionAPI->>IngestionAPI: validatePhysicalInventoryHasNotProcessStarted()
    IngestionAPI->>IngestionAPI: createIngestionReportDto()
    loop For each offer
        IngestionAPI->>Kafka: sendOffer(offerDto)
    end
    IngestionAPI->>Kafka: sendIngestionJobReport(STARTED)
    IngestionAPI-->>Client: 200 OK + reportId
    Kafka->>lcAPI: IngestionReportMsg
    lcAPI->>lcAPI: createWorkflow()
```

---

### 4.2 Stream Upload (Large Files)

**Use case**: Files of any size (tested up to 2 GB+) without loading the entire file into memory.

**Characteristics**:
- Processed via **Spring Batch** with `OfferStreamItemReader` and `OfferItemWriter`.
- Configurable chunk size (`ingestion.batch.chunk-size`, default 10).
- Configurable skip limit (`ingestion.batch.skip-limit`, default 2 in current config).
- Constant memory usage regardless of file size.
- Processed on a **virtual thread** (`Thread.ofVirtual()`) to avoid blocking the main thread.

**Flow**:
```mermaid
sequenceDiagram
    participant Client
    participant IngestionAPI
    participant SpringBatch
    participant MongoDB
    participant Kafka

    Client->>IngestionAPI: POST /stream (multipart)
    IngestionAPI->>IngestionAPI: validatePhysicalInventoryHasNotProcessStarted()
    IngestionAPI->>IngestionAPI: createIngestionReportDto()
    IngestionAPI->>Kafka: sendIngestionJobReport(STARTED)
    IngestionAPI-->>Client: 202 Accepted

    IngestionAPI->>SpringBatch: jobLauncher.run(offerIngestionJob, params)
    loop Chunks of N records
        SpringBatch->>SpringBatch: OfferStreamItemReader.read()
        SpringBatch->>SpringBatch: OfferItemWriter.write()
        SpringBatch->>MongoDB: Save Draft Offers
    end
    SpringBatch->>Kafka: sendBatchReport(COMPLETED/FAILED)
```

---

### 4.3 Remote URL (Pull Model)

**Use case**: Files hosted on partner systems or cloud storage.

**Characteristics**:
- The system downloads the file from the provided URL using `HttpClient` with automatic redirect following.
- Connection timeout: 10 seconds; download timeout: 5 minutes.
- Processed on a virtual thread to avoid blocking the server.
- Once downloaded, reuses the same stream pipeline (`processOffersStream`).

**Flow**:
```mermaid
sequenceDiagram
    participant Client
    participant IngestionAPI
    participant ExternalURL
    participant SpringBatch

    Client->>IngestionAPI: POST /url {url, format, inventoryId}
    IngestionAPI->>IngestionAPI: validateUrl() + validateInventory()
    IngestionAPI-->>Client: 202 Accepted
    IngestionAPI->>ExternalURL: GET (HttpClient, streaming)
    ExternalURL-->>IngestionAPI: InputStream
    IngestionAPI->>SpringBatch: processOffersStream()
```

---

### 4.4 Directory Monitor (Automated)

**Use case**: Automatic processing of files deposited in monitored folders.

**Characteristics**:
- Automatically detects new files.
- Supports network paths and SFTP locations.
- Archives or deletes files after processing.

---

## 5. Asynchronous Processing Pipeline (Spring Batch)

### 5.1 Job Configuration (`IngestionBatchConfig`)

```mermaid
graph LR
    A[OfferStreamItemReader] -->|chunk of N records| B[OfferItemWriter]
    B --> C{More data?}
    C -->|Yes| A
    C -->|No| D[JobCompletionNotificationListener]
    D --> E[Kafka: sendBatchReport]
```

### 5.2 Retry Strategy

The ingestion step implements a layered fault-tolerance policy:

| Layer | Mechanism | Configuration |
|-------|-----------|---------------|
| **Retry** | Retries infrastructure errors (Kafka, DB) | 3 attempts, exponential backoff (2s, 4s, 8s) |
| **Skip** | Skips records with data conversion errors | Up to `skip-limit` records |
| **Fail** | Stops the entire Job | Critical infrastructure errors or skip-limit exceeded |

```mermaid
flowchart TD
    A[Process Record] --> B{Error?}
    B -->|No| C[Write to MongoDB]
    B -->|Yes| D{LCIngestionException?}
    D -->|CONVERSION_ERROR| E{skipCount <= skipLimit?}
    E -->|Yes| F[Skip + log failedId]
    E -->|No| G[FAIL Job]
    D -->|DATABASE/INFRA error| H{Retries < 3?}
    H -->|Yes| I[Retry with exponential backoff]
    H -->|No| G
    D -->|Other error| G
```

### 5.3 Configuration Parameters (application.yml)

```yaml
ingestion:
  batch:
    chunk-size: 10       # Records per chunk
    skip-limit: 2        # Max skipped records before failing the Job
```

---

## 6. Interaction with the lcAPI Monolith — State Machine

### 6.1 Workflow Overview (`MICRO_ING_SM`)

The `lcAPI` monolith orchestrates the lifecycle of each ingestion through a state machine managed by `MicroIngestionWorkflowDispatcher`. Each ingestion report has its own workflow instance.

```mermaid
stateDiagram-v2
    [*] --> STARTED : Report received (STARTING/STARTED)
    STARTED --> ST_CHECK_INGESTION_REQUEST : EV_STARTED

    ST_CHECK_INGESTION_REQUEST --> ST_PUBLISH_ALL : EV_PERSISTANCE_SUCCESS
    ST_CHECK_INGESTION_REQUEST --> ST_CLEAN_PUBLISHING : EV_PERSISTANCE_FAIL

    ST_PUBLISH_ALL --> END : EV_INGESTION_PUBLISHED
    ST_PUBLISH_ALL --> END : EV_INGESTION_NOT_PUBLISHED

    ST_CLEAN_PUBLISHING --> END : EV_JOB_CLEANED
    ST_CLEAN_PUBLISHING --> END : EV_JOB_NOT_CLEANED

    END --> [*]
```

### 6.2 State Descriptions

| State | Responsibility |
|-------|----------------|
| `STARTED` | Initial state. Receives the ingestion token (`VAR_INGESTION_TOKEN`) and fires `EV_STARTED`. |
| `ST_CHECK_INGESTION_REQUEST` | Validates that all offers have been persisted in MongoDB. Waits for success or failure signal. |
| `ST_PUBLISH_ALL` | Publishes the offers: sends a Kafka **promote** event (`sendPromoteAction`). Transitions to END. |
| `ST_CLEAN_PUBLISHING` | Aborts the ingestion: sends a Kafka **delete** event (`sendDeleteAction`). Transitions to END. |
| `END` | Terminal state. No further actions. |

### 6.3 Complete Messaging Flow Between ingestion-api and lcAPI

```mermaid
sequenceDiagram
    participant IngestionAPI
    participant Kafka
    participant lcAPI as lcAPI (IngestionMicroService)
    participant Dispatcher as WorkflowDispatcher
    participant SM as State Machine

    IngestionAPI->>Kafka: sendIngestionJobReport(STARTED)
    Kafka->>lcAPI: IngestionReportMsg
    lcAPI->>Dispatcher: createWorkflow(MICRO_ING_SM)
    Dispatcher->>SM: EV_STARTED → ST_CHECK_INGESTION_REQUEST

    Note over IngestionAPI: Batch Job processing...

    IngestionAPI->>Kafka: sendIngestionJobReport(COMPLETED + processed=true)
    Kafka->>lcAPI: IngestionReportMsg
    lcAPI->>Dispatcher: dispatchWorkflow(EV_PERSISTANCE_SUCCESS)
    Dispatcher->>SM: ST_PUBLISH_ALL
    Dispatcher->>Kafka: sendPromoteAction(ingestionReportId)

    Kafka->>IngestionAPI: IngestionReportActionMsg (promote)
    IngestionAPI->>IngestionAPI: promoteDraftOffersToVehicleOffers()
    IngestionAPI->>Kafka: sendIngestionJobReport(promoted=true)
    IngestionAPI->>Kafka: sendIngestionReportPromoteActionNotification(SUCCESS)

    Kafka->>lcAPI: IngestionReportResponseActionMsg
    lcAPI->>Dispatcher: processIngestionReportPromoteAction()
    Dispatcher->>SM: EV_INGESTION_PUBLISHED → END
```

### 6.4 Error and Cleanup Flow

```mermaid
sequenceDiagram
    participant IngestionAPI
    participant Kafka
    participant lcAPI
    participant SM as State Machine

    IngestionAPI->>Kafka: sendIngestionJobReport(FAILED)
    Kafka->>lcAPI: IngestionReportMsg (FAILED/ABANDONED)
    lcAPI->>SM: EV_PERSISTANCE_FAIL → ST_CLEAN_PUBLISHING
    SM->>Kafka: sendDeleteAction(ingestionReportId)

    Kafka->>IngestionAPI: IngestionReportActionMsg (delete)
    IngestionAPI->>IngestionAPI: deleteDraftOffersByIngestionReportId()
    IngestionAPI->>Kafka: sendIngestionReportDeleteActionNotification(SUCCESS/FAILED)

    Kafka->>lcAPI: IngestionReportResponseActionMsg
    lcAPI->>SM: EV_JOB_CLEANED → END
```

---

## 7. Synchronization and Schedulers

The system includes schedulers to guarantee consistency between the database and the actual state of jobs:

| Scheduler | Cron | Lock | Purpose |
|-----------|------|------|---------|
| `syncPendingBatchReports` | Every 30 seconds | `IngestionBatchReportsSync_Lock` (4m/1m) | Checks pending batch reports and processes them when MongoDB count matches |
| `syncPendingReports` | Every 30 seconds | `IngestionReportsSync_Lock` (4m/1m) | Same for PROCESS-type ingestion reports (API) |
| `purgeScheduler` | Sundays at 3 AM | — | Purges old records (> 7 days) |

Locks are managed with **ShedLock** to ensure only one replica executes each scheduler in multi-instance environments.

---

## 8. Data Model and Storage

### 8.1 Multi-Storage Strategy

```mermaid
graph TD
    A[Ingestion API] -->|Draft Offers| B[(MongoDB\ningestion_service)]
    A -->|Reports & Batch metadata| C[(PostgreSQL\nliquidcars)]
    A -->|Promoted Offers| D[(MongoDB\nFinal Offers)]
    A -->|Messaging| E[Apache Kafka]
```

| Store | Purpose | Collection/Table |
|-------|---------|-----------------|
| **MongoDB** | Draft offers awaiting promotion, flexible schema | `draft_offers` |
| **PostgreSQL** | Ingestion reporting, Spring Batch metadata | `ingestion_reports`, `BATCH_*` |
| **MongoDB** | Final promoted offers, source of truth for queries | `offers` |

### 8.2 Upsert Logic and Deduplication

- Unique offer identifier (`externalPublicationId`) to prevent duplicates.
- Multiple ingestions of the same data result in updates, not duplicates.
- `DumpType` determines whether the ingestion is incremental or a full dump.

### 8.3 Promoting Drafts to Active Offers

```mermaid
flowchart LR
    A[Draft in MongoDB] -->|promoteDraftOffersToVehicleOffers| B{Already promoted?}
    B -->|No| C[Apply DumpType]
    C --> D[Respect actively booked offers]
    D --> E[Write to Final MongoDB Collection]
    E --> F[Delete drafts from MongoDB]
    F --> G[Notify via Kafka]
    B -->|Yes| H[Ignore - already promoted]
```

---

## 9. Error Handling and Recovery

### 9.1 Fault Tolerance Strategies

**Invalid record (data error)**
- The record is skipped and its identifier is logged (`failedExternalIds`).
- Processing continues with the remaining records.
- Failed IDs are included in the final report.

**Infrastructure error (Kafka, DB)**
- Automatic retry (3 attempts, exponential backoff: 2s → 4s → 8s).
- If the error persists, the Job fails completely and the cleanup flow is triggered.

**Skip-limit exceeded**
- If more than `skip-limit` records are skipped, the Job fails.
- The cleanup flow is triggered via Kafka → lcAPI → `ST_CLEAN_PUBLISHING`.

### 9.2 Concurrency Validation

Before starting any ingestion, the system verifies that no active process already exists for the same inventory:

```java
// Final statuses that allow a new ingestion
List<IngestionBatchStatus> finalStatuses = List.of(
    COMPLETED, FAILED, ABANDONED, STOPPED
);
// If a report exists in a non-final status → exception
```

---

## 10. Security

### 10.1 Authentication and Authorization

- All endpoints require JWT authentication via **Keycloak** (`security.security-profile-issuer`).
- Role-based authorization using `@LCFunctionalContext` annotations.
- Partners can only access their own data; admins have cross-partner visibility.

### 10.2 Request Size Limits

- Batch and URL endpoints have a configurable size restriction (`max-batch-size`, default 10 MB).
- Affected paths: `/batch`, `/url`.
- Multipart with no limit (`max-file-size: -1`) for large stream uploads.

### 10.3 Data in Transit and at Rest

- Encrypted communications in transit (HTTPS + Kafka with TLS).
- Credentials externalized via environment variables (`SPRING_DATA_MONGODB_URI`, `SPRING_DATASOURCE_URL`, `KAFKA_BOOTSTRAP_SERVERS`).

---

## 11. Observability

### 11.1 Monitoring Endpoints

Actuator exposed at:
- `/actuator/health` — Liveness and Readiness probes (Kubernetes-ready).
- `/actuator/metrics` — JVM and application metrics.
- `/actuator/prometheus` — Prometheus format for scraping.

### 11.2 Logging

| Component | Default Level |
|-----------|---------------|
| Spring Batch | INFO |
| Kafka | WARN |
| Hibernate SQL | WARN |
| Liquibase | INFO |
| Application (`com.orbyn.ingestion`) | WARN |

### 11.3 API Documentation

Swagger UI available at `/swagger-ui.html`, with the OpenAPI specification loaded from `/api/ingestion-api.yml`.

---

## 12. Expected Performance

| Scenario | Volume | Estimated Time | Memory |
|----------|--------|----------------|--------|
| Small API batch | 1,000 records | < 30 seconds | Minimal |
| Medium upload | 10,000 records | 2–5 minutes | ~256 MB |
| Large stream | 100,000 records | 15–25 minutes | ~256 MB |
| Massive bulk load | 170,000 records | 25–40 minutes | ~256 MB |

**Horizontal scaling**: adding Spring Batch workers reduces processing time linearly:
- 2 workers → 170,000 records in ~30 min
- 4 workers → ~15 min
- 8 workers → ~8 min

---

## 13. Infrastructure Configuration (application.yml)

### 13.1 Key Configuration Summary

| Section | Key Parameter | Default Value |
|---------|--------------|---------------|
| Server | `server.port` | 8890 |
| Multipart | `max-file-size` | Unlimited (-1) |
| Batch chunk | `ingestion.batch.chunk-size` | 10 |
| Batch skip | `ingestion.batch.skip-limit` | 2 |
| Kafka producer acks | `acks` | all (maximum durability) |
| Kafka consumer retries | `max-attempts` | 5 |
| HikariCP pool | `maximum-pool-size` | 10 |
| Purge scheduler | Cron | Sundays at 3 AM |
| Sync scheduler | Cron | Every 30 seconds |

---

## 14. Success Criteria

The system will be considered successful when:

- ✅ Imports of 170,000+ records complete without system restart
- ✅ Memory usage remains constant regardless of file size
- ✅ Failed jobs can be resumed without reprocessing already completed data
- ✅ 99.9% of valid records are processed successfully
- ✅ The lcAPI workflow reflects the correct state at each phase
- ✅ Zero data loss during the ingestion process
- ✅ Complete visibility of all ingestion operations via Kafka + PostgreSQL

---

## 15. Glossary

| Term | Definition |
|------|-----------|
| **Draft Offer** | Offer persisted in MongoDB awaiting promotion |
| **Promoted Offer** | Offer visible in Elasticsearch, accessible by end users |
| **DumpType** | Ingestion type: incremental (changes only) or full (replaces everything) |
| **Chunk** | Subset of records processed together as an atomic unit |
| **Skip** | Record omitted due to a non-critical data error |
| **Workflow (MICRO_ING_SM)** | State machine in lcAPI that orchestrates the lifecycle of each ingestion |
| **ShedLock** | Distributed locking mechanism for schedulers in multi-replica environments |
| **Upsert** | Operation that updates if a record exists or inserts it if it does not |
| **VAR_INGESTION_TOKEN** | Workflow variable referencing the ingestion report ID |
