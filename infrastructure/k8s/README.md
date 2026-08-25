# Running Chaos Stream on Kubernetes

These manifests deploy the core pipeline — 5 app services + Postgres/Redis/Kafka/kafka-ui —
to a local cluster (kind or Minikube). The monitoring stack (Prometheus/Grafana/Loki/Promtail)
is docker-compose-only for now; Promtail specifically needs a real rewrite (DaemonSet +
Kubernetes-API service discovery) to work in a cluster, since it relies on the Docker socket
in docker-compose, which doesn't exist the same way here.

## How this differs from docker-compose

- **No image registry.** Kubernetes only pulls images, it never builds them. You build locally
  with `docker compose build` (same as always) and then load those exact images into the
  cluster directly — see step 2 below.
- **No `depends_on`.** Nothing here waits for anything else to start. Instead, every Spring
  Boot service has a readiness probe (`/actuator/health/readiness`) that only starts passing
  once the app is actually up, and the apps themselves retry their Kafka/Postgres connections
  on startup the same way they always have. A pod can restart/reschedule at any time — that's
  normal, not a sign of failure — the whole point of readiness probes is Kubernetes not sending
  traffic to a pod until it's actually ready.
- **Postgres and Kafka are StatefulSets with real persistent storage** — an improvement over
  docker-compose, which currently has no volumes for either (data is lost on `docker compose
  down`; here it survives pod restarts, if not the cluster being deleted).

## 1. Build the images

```bash
cd services
docker compose build
```

This produces `chaos-stream-auth-service:latest`, `chaos-stream-gateway-service:latest`,
`chaos-stream-ingestion-service:latest`, `chaos-stream-validation-service:latest`, and
`chaos-stream-storage-service:latest` locally — exactly what the manifests here reference.

## 2. Load the images into your cluster

Kubernetes can't see your local Docker image cache by default — you have to load images in
explicitly (or push to a registry, which is overkill for local dev).

**If you're using kind:**

```bash
for svc in auth-service gateway-service ingestion-service validation-service storage-service; do
  kind load docker-image chaos-stream-${svc}:latest
done
```

**If you're using Minikube:**

```bash
for svc in auth-service gateway-service ingestion-service validation-service storage-service; do
  minikube image load chaos-stream-${svc}:latest
done
```

## 3. Apply the manifests

```bash
kubectl apply -f infrastructure/k8s/
```

Watch everything come up:

```bash
kubectl get pods -n chaos-stream -w
```

Postgres and Kafka take the longest to become `Ready` (health checks + PVC provisioning) —
everything else waits on nothing, so don't be surprised if app pods restart once or twice
early on while their dependencies aren't up yet. That's expected; Kubernetes will keep
retrying.

## 4. Reach it from your laptop

Every Service here is ClusterIP-only (not reachable from outside the cluster directly) —
`kubectl port-forward` works identically on kind and Minikube, so that's what we use instead
of NodePort:

```bash
kubectl port-forward -n chaos-stream svc/gateway-service 8080:8080
kubectl port-forward -n chaos-stream svc/auth-service 9000:9000
kubectl port-forward -n chaos-stream svc/kafka-ui 8085:8080
```

Run each in its own terminal (or background them with `&`). Once forwarded, everything works
exactly like the docker-compose setup — see `POSTMAN_TESTING.md` at the repo root for the full
walkthrough (get a token from `localhost:9000`, POST/GET transactions through `localhost:8080`).

## 5. Tear down

```bash
kubectl delete -f infrastructure/k8s/
```

Note this does **not** delete the PVCs (`postgres-data`/`kafka-data`) by default — that's
intentional (protects against accidental data loss). To actually wipe the data too:

```bash
kubectl delete pvc -n chaos-stream --all
kubectl delete namespace chaos-stream
```
