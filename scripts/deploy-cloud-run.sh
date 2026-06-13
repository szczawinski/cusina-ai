#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${PROJECT_ID:-}" || -z "${REGION:-}" || -z "${SERVICE_NAME:-}" || -z "${ANTHROPIC_API_KEY:-}" ]]; then
  echo "Ustaw PROJECT_ID, REGION, SERVICE_NAME i ANTHROPIC_API_KEY." >&2
  exit 1
fi

MAX_INSTANCES="${MAX_INSTANCES:-1}"
MIN_INSTANCES="${MIN_INSTANCES:-0}"
ANTHROPIC_MODEL="${ANTHROPIC_MODEL:-claude-haiku-4-5}"

IMAGE="gcr.io/${PROJECT_ID}/${SERVICE_NAME}:latest"

gcloud builds submit --tag "${IMAGE}"

gcloud run deploy "${SERVICE_NAME}" \
  --image "${IMAGE}" \
  --platform managed \
  --region "${REGION}" \
  --allow-unauthenticated \
  --set-env-vars "ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY},ANTHROPIC_MODEL=${ANTHROPIC_MODEL},SPRING_PROFILES_ACTIVE=cloud" \
  --min-instances="${MIN_INSTANCES}" \
  --max-instances="${MAX_INSTANCES}"

echo "Wdrożenie zakończone. Sprawdź: /actuator/health oraz /actuator/health/readiness"

