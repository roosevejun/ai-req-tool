# K8s Deployment (Backend + Frontend)

This directory is generated based on your existing style (`namespace: ai-req-tool`, `nginx ingress`, `cert-manager`).

## Files

- `00-namespace.yaml`
- `01-secrets-and-config.example.yaml`
- `02-image-pull-secret.example.yaml`
- `10-backend.yaml`
- `11-frontend.yaml`
- `12-templates-configmap.yaml`
- `20-ingress.yaml`

## Before Apply

1. Replace secret values in `01-secrets-and-config.example.yaml`.
   - `DB_URL` must be a complete JDBC URL, for example:
     - `jdbc:postgresql://192.168.20.201:5432/ai-req-tool`
   - If non-structured files should be stored in MinIO, fill:
     - `MINIO_ACCESS_KEY`
     - `MINIO_SECRET_KEY`
   - Check backend config values:
     - `STORAGE_PROVIDER`
     - `MINIO_ENDPOINT`
     - `MINIO_BUCKET`
2. Create Harbor image pull secret:
   - Either use `02-image-pull-secret.example.yaml`
   - Or run:
     - `kubectl -n ai-req-tool create secret docker-registry harbor-pull-secret --docker-server=harbor.ahtongtu.cn --docker-username=<user> --docker-password=<password>`
3. Check ingress hosts in `20-ingress.yaml`:
   - `aitool.ahtongtu.cn`
   - `aitoolapi.ahtongtu.cn`

## Apply Order

```bash
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-secrets-and-config.example.yaml
kubectl apply -f 02-image-pull-secret.example.yaml
kubectl apply -f 12-templates-configmap.yaml
kubectl apply -f 10-backend.yaml
kubectl apply -f 11-frontend.yaml
kubectl apply -f 20-ingress.yaml
```

## Current Deployment Notes

- Backend now supports MinIO-backed knowledge file storage.
- `10-backend.yaml` injects MinIO environment variables from the backend secret/config.
- Backend and frontend now include basic readiness/liveness probes and resource requests/limits.
- Backend still mounts `/app/data` as an `emptyDir` so local fallback storage can continue working if `STORAGE_PROVIDER=local`.


