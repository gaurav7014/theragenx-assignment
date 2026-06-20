#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${SCRIPT_DIR}/../backend"
COMPOSE_FILE="${BACKEND_DIR}/docker-compose.yml"
SERVICE_NAME="pv-service"
BASE_URL="http://localhost:8080/api/v1"
HEALTH_URL="${BASE_URL}/health"

# ── helpers ───────────────────────────────────────────────────────────────────

die() { echo "error: $*" >&2; exit 1; }

check_docker() {
    docker info > /dev/null 2>&1 \
        || die "Docker is not running — start Docker and retry."
}

run_compose() {
    docker compose -f "${COMPOSE_FILE}" "$@"
}

# ── subcommands ───────────────────────────────────────────────────────────────

cmd_build() {
    if [ "${1:-}" = "--help" ]; then
        cat <<EOF
Usage: $(basename "$0") build

Build the Docker image for ${SERVICE_NAME} using the Dockerfile in backend/.
Equivalent to: docker compose build
EOF
        return 0
    fi
    check_docker
    run_compose build
}

cmd_start() {
    if [ "${1:-}" = "--help" ]; then
        cat <<EOF
Usage: $(basename "$0") start

Bring the stack up in detached mode and wait for the healthcheck to pass.
Prints the base URL once the service is ready.
EOF
        return 0
    fi
    check_docker
    run_compose up -d

    echo "Waiting for ${SERVICE_NAME} to become healthy..."
    local attempt=0
    local max_attempts=30   # 30 × 2 s = 60 s max
    while [ "${attempt}" -lt "${max_attempts}" ]; do
        local container_id
        container_id="$(run_compose ps -q "${SERVICE_NAME}" 2>/dev/null || true)"
        if [ -n "${container_id}" ]; then
            local health
            health="$(docker inspect \
                --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
                "${container_id}" 2>/dev/null || echo "unknown")"
            if [ "${health}" = "healthy" ]; then
                echo "Service is up → ${BASE_URL}"
                return 0
            fi
        fi
        attempt=$((attempt + 1))
        sleep 2
    done
    die "Service did not become healthy within 60 s. Check logs with: $(basename "$0") logs"
}

cmd_stop() {
    if [ "${1:-}" = "--help" ]; then
        cat <<EOF
Usage: $(basename "$0") stop

Stop and remove the service containers.
Equivalent to: docker compose down
EOF
        return 0
    fi
    check_docker
    run_compose down
}

cmd_test() {
    if [ "${1:-}" = "--help" ]; then
        cat <<EOF
Usage: $(basename "$0") test

Run the Gradle test suite on the host (not inside a container).
Requires: JDK 17+ available, Gradle wrapper present in backend/.
EOF
        return 0
    fi
    cd "${BACKEND_DIR}"
    ./gradlew test
}

cmd_logs() {
    if [ "${1:-}" = "--help" ]; then
        cat <<EOF
Usage: $(basename "$0") logs

Tail logs from the running ${SERVICE_NAME} container (Ctrl-C to stop).
EOF
        return 0
    fi
    check_docker
    run_compose logs -f "${SERVICE_NAME}"
}

cmd_clean() {
    if [ "${1:-}" = "--help" ]; then
        cat <<EOF
Usage: $(basename "$0") clean

Remove containers, images built by compose, and Gradle build artifacts.
  - docker compose down --rmi all --volumes --remove-orphans
  - rm -rf backend/build  backend/.gradle
EOF
        return 0
    fi
    check_docker
    run_compose down --rmi all --volumes --remove-orphans

    echo "Removing Gradle build artifacts..."
    rm -rf "${BACKEND_DIR}/build" "${BACKEND_DIR}/.gradle"
    echo "Clean complete."
}

# ── usage ────────────────────────────────────────────────────────────────────

usage() {
    cat <<EOF
Usage: $(basename "$0") <subcommand> [--help]

Subcommands:
  build    Build the Docker image
  start    Start the stack and wait for it to be healthy
  stop     Stop and remove containers
  test     Run unit tests on the host
  logs     Tail container logs
  clean    Remove containers, images, and build artifacts

Run '$(basename "$0") <subcommand> --help' for details on each subcommand.
EOF
}

# ── dispatch ─────────────────────────────────────────────────────────────────

case "${1:-}" in
    build)       shift; cmd_build  "$@" ;;
    start)       shift; cmd_start  "$@" ;;
    stop)        shift; cmd_stop   "$@" ;;
    test)        shift; cmd_test   "$@" ;;
    logs)        shift; cmd_logs   "$@" ;;
    clean)       shift; cmd_clean  "$@" ;;
    --help|-h|help) usage; exit 0 ;;
    "")          usage; exit 2 ;;
    *)
        echo "error: unknown subcommand '${1}'" >&2
        echo >&2
        usage >&2
        exit 2
        ;;
esac
