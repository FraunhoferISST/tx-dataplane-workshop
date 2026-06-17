#!/bin/sh

kind create cluster --config deployment/kind-cluster-config.yaml

helm repo add traefik https://traefik.github.io/charts
helm repo update
helm upgrade --install --namespace traefik traefik traefik/traefik --create-namespace -f deployment/traefik-values.yaml

kubectl apply --server-side --force-conflicts -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.5.1/standard-install.yaml

kind load docker-image controlplane:latest dataplane:latest identityhub:latest issuerservice:latest
