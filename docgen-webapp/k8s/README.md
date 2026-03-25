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


