#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

PIDS=()
LOG_DIR="${ROOT}/logs"
PORTS=(8080 8081 5173)
CLEANUP_PORTS=0

port_pids() {
  local port="$1"
  powershell.exe -NoProfile -ExecutionPolicy Bypass -Command \
    "\$ErrorActionPreference='SilentlyContinue'; Get-NetTCPConnection -State Listen -LocalPort ${port} | Select-Object -ExpandProperty OwningProcess -Unique" \
    2>/dev/null | tr -d '\r' | sed '/^$/d' || true
}

check_ports_available() {
  local used=0
  for port in "${PORTS[@]}"; do
    local pids
    pids="$(port_pids "${port}")"
    if [[ -n "${pids}" ]]; then
      echo "Port ${port} is already in use by PID(s): ${pids//$'\n'/, }" >&2
      used=1
    fi
  done

  if [[ "${used}" -ne 0 ]]; then
    echo "Refusing to start. Stop the process above or change the service port first." >&2
    exit 1
  fi
}

stop_port_listeners() {
  local port
  for port in "${PORTS[@]}"; do
    local pids
    pids="$(port_pids "${port}")"
    if [[ -n "${pids}" ]]; then
      while IFS= read -r pid; do
        [[ -z "${pid}" ]] && continue
        echo "Stopping listener on port ${port}: PID ${pid}"
        powershell.exe -NoProfile -ExecutionPolicy Bypass -Command \
          "Stop-Process -Id ${pid} -Force -ErrorAction SilentlyContinue" \
          >/dev/null 2>&1 || true
      done <<<"${pids}"
    fi
  done
}

wait_for_ports() {
  local deadline=$((SECONDS + 120))
  local missing=()

  while (( SECONDS < deadline )); do
    missing=()
    for port in "${PORTS[@]}"; do
      if [[ -z "$(port_pids "${port}")" ]]; then
        missing+=("${port}")
      fi
    done

    if [[ ${#missing[@]} -eq 0 ]]; then
      return 0
    fi

    sleep 2
  done

  echo "Timed out waiting for port(s): ${missing[*]}" >&2
  exit 1
}

monitor_ports() {
  while true; do
    sleep 5
    for port in "${PORTS[@]}"; do
      if [[ -z "$(port_pids "${port}")" ]]; then
        echo "Port ${port} is no longer listening. Stopping debug services." >&2
        exit 1
      fi
    done
  done
}

cleanup() {
  local code=$?
  if [[ ${#PIDS[@]} -gt 0 ]]; then
    echo
    echo "Stopping debug processes..."
    for pid in "${PIDS[@]}"; do
      if kill -0 "${pid}" 2>/dev/null; then
        kill "${pid}" 2>/dev/null || true
      fi
    done
    wait "${PIDS[@]}" 2>/dev/null || true
  fi
  if [[ "${CLEANUP_PORTS}" -eq 1 ]]; then
    stop_port_listeners
  fi
  exit "${code}"
}

trap cleanup INT TERM EXIT

run_bg() {
  local name="$1"
  local log_file="$2"
  shift
  shift
  echo "Starting ${name}: $*"
  echo "  log: ${log_file}"
  (
    cd "${ROOT}"
    "$@"
  ) >"${log_file}" 2>&1 &
  PIDS+=("$!")
}

echo "Ass Kicker local debug"
echo "Manager: http://localhost:8080"
echo "Worker:  http://localhost:8081"
echo "UI:      http://localhost:5173"
echo

check_ports_available

mkdir -p "${LOG_DIR}"
: >"${LOG_DIR}/manager.log"
: >"${LOG_DIR}/worker.log"
: >"${LOG_DIR}/ui.log"

echo "Preparing common module..."
mvn -f "${ROOT}/svr/common/pom.xml" clean install -DskipTests
echo

CLEANUP_PORTS=1
run_bg "manager" "${LOG_DIR}/manager.log" mvn -f "${ROOT}/svr/pom.xml" -pl manager -Dexec.skip=true clean spring-boot:run
run_bg "worker" "${LOG_DIR}/worker.log" mvn -f "${ROOT}/svr/pom.xml" -pl worker clean spring-boot:run
run_bg "ui" "${LOG_DIR}/ui.log" npm --prefix "${ROOT}/ui" run dev -- --host 0.0.0.0 --strictPort

echo
echo "Waiting for debug ports..."
wait_for_ports

echo
echo "All debug processes started. Press Ctrl+C to stop."
echo "Logs:"
echo "  ${LOG_DIR}/manager.log"
echo "  ${LOG_DIR}/worker.log"
echo "  ${LOG_DIR}/ui.log"

monitor_ports
