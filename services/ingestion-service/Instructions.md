Phase-by-phase workflow

Phase 1 — Producer plumbing
- Add spring-kafka to ingestion-service/pom.xml (keep it service-local, not common-library — common-library is a model/exception-handling library, not a messaging one; validation/storage will add their own consumer-side dependency later).
- Add a spring.kafka.bootstrap-servers + producer serializer config block to application.yaml (the env var SPRING_KAFKA_BOOTSTRAP_SERVERS is already wired in docker-compose, currently inert — this is what activates it).
- Define your topic name as a constant somewhere sensible, and create the NewTopic bean per your decision above.
- Checkpoint: app should start clean against the docker-compose Kafka broker with no producer code yet — just confirm in kafka-ui that your topic gets created on boot.

Phase 2 — The ingestion endpoint
- POST /api/v1/transactions (matches the gateway's predicate exactly, no path rewriting — note this is a write, so the gateway requires SCOPE_message.write when called through the gateway).
- Accept @Valid @RequestBody TransactionEvent — this is your "syntactic validation" for free, since GlobalExceptionHandler in common-library already turns a failed @Valid into a 400 VALIDATION_FAILED response with per-field messages (you built this two turns ago).
- Decide and implement the response shape on success — I'd suggest 202 Accepted with just the event_id echoed back, since you're a buffer, not the system of record.
- Checkpoint: before touching Kafka, get this returning 400s correctly for malformed bodies (missing fields, negative amount, etc.) using the validation you already have. That's a fast win and isolates the "syntactic validation" half of the README's description from the "Kafka" half.

Phase 3 — Wire the producer into the endpoint
- Send the validated event to Kafka using your chosen key/send-semantics.
- Handle the failure path: if the bounded wait times out or the broker's unreachable, map it to an ApplicationException — you'll likely want a new ErrorCode (nothing in the enum currently fits "couldn't reach the broker" — SERVICE_UNAVAILABLE is the closest existing one; decide whether to reuse it or add e.g. INGESTION_FAILED).
- Checkpoint: manually stop the Kafka container (docker compose stop kafka) while ingestion-service is running and confirm a POST now returns your chosen 5xx with a proper GlobalErrorResponse body, not a raw stack trace.

Phase 4 — Automated tests
- Unit-level: @WebMvcTest (or slice test) on the controkay ller with a mocked KafkaTemplate, covering: valid payload → 202, invalid payload → 400 with expected validationErrors, producer failure → your chosen 5xx.