## "Chaos Stream" - Event-Driven Financial Audit System

A system that consumes a high-volume stream of "transactions" (can be simulated), validates them against business rules,
and stores them in a tamper-proof ledger.
The goal is to handle a stream of financial events that must be processed in order, validated, and stored with high
reliability.

---

### Microservices Architecture & Design Patterns

The system is built on a Decoupled Event-Driven Architecture designed for high throughput and fault tolerance. Unlike a
traditional monolithic approach, the "Chaos Stream" separates concerns across four specialized layers:

Edge Layer: A Reverse Proxy/Gateway acts as the single entry point, delegating authentication to a stateless Auth
Service to ensure only verified JWT traffic enters the cluster.

Ingestion Layer: A Spring Boot Ingestion Service performs rapid syntactic validation (schema checks) before offloading
raw events to Kafka. This acts as a buffer, protecting downstream services from traffic spikes.

Processing Layer: The Validation Service consumes events asynchronously. It implements complex business logic using Java
21 Sealed Classes for type-safe transaction handling and utilizes the Dead Letter Queue (DLQ) pattern to isolate
malformed business data without halting the pipeline.

Persistence Layer: The Audit Service ensures data integrity by persisting validated transactions into PostgreSQL. It
implements the Idempotent Consumer pattern to prevent duplicate entries in the event of network retries.

### Project Structure

```chaos-stream/
├── .github/workflows/       # CI/CD pipelines
├── services/                # Application Code
│   ├── ingestion-service/   # Spring Boot Serivce
│   ├── validation-service/  # Spring Boot Serivce
│   ├── storage-service/     # Spring Boot Serivce
│   └── auth-service/        # Spring Boot Serivce
├── infrastructure/          # K8s & Cloud
│   ├── k8s/                 # YAML manifests (Deployments, Services)
│   ├── monitoring/          # Grafana dashboards, Prometheus configs
│   └── kafka/               # Kafka scripts and topic configs
├── scripts/                 # Setup and "Chaos" simulation scripts
└── docker-compose.yml       # For quick local testing without K8s
```

---

### Technology Stack

* Spring Boot 3 - Framework
* Java 21 - Language
* Kafka - Event store
* Docker - Containerization
* K8s - Orchestration
* Prometheus & Grafana - Observability
* CI/CD - Github Actions

<img width="1741" height="752" alt="sceenshot" src="https://github.com/user-attachments/assets/0668412c-9622-42a7-a36d-e0118a1db224" />

