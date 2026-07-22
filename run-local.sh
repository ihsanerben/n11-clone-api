#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy .env.example to .env and fill in the values." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

required_variables=(
  DB_URL
  DB_USERNAME
  DB_PASSWORD
  SERVER_PORT
  CORS_ALLOWED_ORIGIN
  FRONTEND_BASE_URL
  COOKIE_SECURE
  JWT_SECRET
  MAIL_HOST
  MAIL_PORT
  MAIL_USERNAME
  MAIL_PASSWORD
  MAIL_FROM
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required variable in .env: ${variable_name}" >&2
    exit 1
  fi
done

if (( ${#JWT_SECRET} < 32 )); then
  echo "JWT_SECRET must be at least 32 characters." >&2
  exit 1
fi

exec "${SCRIPT_DIR}/mvnw" spring-boot:run -Dspring-boot.run.profiles=local "$@"
