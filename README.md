## "Chaos Stream" - Event-Driven Financial Audit System

A system that consumes a high-volume stream of "transactions" (can be simulated), validates them against business rules, and stores them in a tamper-proof ledger.
The goal is to handle a stream of financial events that must be processed in order, validated, and stored with high reliability.

---

### Microservices Architecture & Design Patterns

The system is built on a Decoupled Event-Driven Architecture designed for high throughput and fault tolerance. Unlike a traditional monolithic approach, the "Chaos Stream" separates concerns across four specialized layers:

Edge Layer: A Reverse Proxy/Gateway acts as the single entry point, delegating authentication to a stateless Auth Service to ensure only verified JWT traffic enters the cluster.

Ingestion Layer: A Spring Boot Ingestion Service performs rapid syntactic validation (schema checks) before offloading raw events to Kafka. This acts as a buffer, protecting downstream services from traffic spikes.

Processing Layer: The Validation Service consumes events asynchronously. It implements complex business logic using Java 21 Sealed Classes for type-safe transaction handling and utilizes the Dead Letter Queue (DLQ) pattern to isolate malformed business data without halting the pipeline.

Persistence Layer: The Audit Service ensures data integrity by persisting validated transactions into PostgreSQL. It implements the Idempotent Consumer pattern to prevent duplicate entries in the event of network retries.

---

### Technology Stack

 * Spring Boot 3 - Framework
 * Java 21 - Language
 * Kafka - Event store
 * Docker - Containerization
 * K8s - Orchestration
 * Prometheus & Grafana - Observability


   <img width="1229" height="603" alt="image" src="https://github.com/user-attachments/assets/17d70519-c896-44b6-9305-531b8f4ed53a" />
