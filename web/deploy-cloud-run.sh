#!/usr/bin/env bash
set -euo pipefail

# Script de déploiement Google Cloud Run — Merélec ERP Web (Windows & macOS)
# Connecté à la base Cloud Firestore : ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4

SERVICE_NAME="merelec-erp-web"
REGION="europe-west2"
PROJECT_ID="gen-lang-client-0345431815"

echo "1. Préparation du build Web..."
cp ../index.html ./index.html

echo "2. Déploiement sur Google Cloud Run ($SERVICE_NAME - $REGION)..."
gcloud run deploy "$SERVICE_NAME" \
  --source . \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --port 8080 \
  --allow-unauthenticated

echo "Déploiement terminé avec succès !"
