# Testing Chaos Stream with Postman

Manual end-to-end test steps for the full pipeline: gateway → auth → ingestion → Kafka → validation → Kafka → storage → Postgres.

## Prerequisites

- Stack running from the repo root: `docker compose up -d --build`
- Postman installed
- The example payload at `input-example.json` (repo root)

## Steps

### 1. Start the stack

```
docker compose up -d --build
```

Wait ~10-15 seconds for all services to finish starting.

### 2. Auth yourself at localhost:9000

- Method: `POST`
- URL: `http://localhost:9000/auth/v1/token`
- Authorization tab → **Basic Auth**: Username `transaction-producer-01`, Password `secret`
- Body tab → **x-www-form-urlencoded**:
  - `grant_type` = `client_credentials`
  - `scope` = `message.read message.write`
- Send. Copy the `access_token` from the response.

**Note:** the token expires after 5 minutes (`expires_in: 299`). If later steps start returning `401`, just repeat this step for a fresh one.

### 3. Submit a transaction at localhost:8080

- Method: `POST`
- URL: `http://localhost:8080/api/v1/transactions`
- Authorization tab → **Bearer Token** → paste the access token from step 2
- Body tab → **raw / JSON** → paste the contents of `input-example.json`
- Send. Expect `202 Accepted` with the `event_id` echoed back in the response body.

### 4. Read the transaction back

- Method: `GET`
- URL: `http://localhost:8080/api/v1/transactions/idem-7b2e1a3c-4d5e-4f7a-8b9c-0d1e2f3a4b5c` (the `idempotency_key` from the payload you sent)
- Authorization tab → **Bearer Token** → same token
- Send. Expect `200 OK` with the full transaction.

This one request proves the whole pipeline worked: ingestion accepted it, validation-service passed it (no business rule violations), and storage-service persisted it to Postgres. It can take a second or two to show up — if you get `404`, wait and retry.

### 5. List everything stored so far

- Method: `GET`
- URL: `http://localhost:8080/api/v1/transactions`
- Authorization tab → **Bearer Token** → same token
- Send. Expect a paginated JSON page (`content`, `totalElements`, etc.) listing every persisted transaction.

### 6. Watch a transaction get rejected (business validation)

- Repeat step 3, but in the body change `metadata.timestamp` to a few minutes in the future and pick a new, unused `idempotency_key`.
- Expect `202 Accepted` again — ingestion only checks field shape (required fields, positive amount, etc.), not business rules.
- Repeat step 4 with the new key — expect `404`. validation-service's `FutureTimestampRule` caught it and routed it to a dead-letter topic instead of storage; it will never appear here.

### 7. Watch duplicate detection kick in

- Repeat step 3 with the **exact same** `idempotency_key` you already successfully used in step 3.
- Expect `202 Accepted` again — ingestion has no concept of duplicates, it just forwards to Kafka.
- Repeat step 5 — the total count should be unchanged. validation-service's `DuplicateTransactionRule` silently drops the reprocessed one before it ever reaches storage.

### 8. Confirm scope enforcement

- Repeat step 2, but request only `scope=message.read` in the body this time.
- Try step 3 (POST) using that token — expect `403 Forbidden`.
- Try step 4 (GET) using that token — still works, since it only needs read access.

### 9. Confirm authentication is required

- Repeat step 3 or step 4 with no `Authorization` header at all — expect `401 Unauthorized`.

## Tip: stop copy-pasting the token

In the step-2 request's **Post-response Script** tab (Postman calls this "Tests" in older versions), add:

```js
pm.collectionVariables.set("access_token", pm.response.json().access_token);
```

Then set every other request's Authorization to **Bearer Token** with the value `{{access_token}}` instead of pasting it in manually each time.

## Shutting down

```
docker compose down
```

Add `-v` to also wipe the Postgres volume and start from a clean database next time.
