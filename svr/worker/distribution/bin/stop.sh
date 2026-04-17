#!/usr/bin/env bash
set -eu
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PIDFILE="${ROOT}/run/ass-kicker.pid"

if [[ ! -f "${PIDFILE}" ]]; then
  echo "pid file not found ${PIDFILE}" >&2
  exit 1
fi

PID="$(cat "${PIDFILE}")"
if ! kill -0 "${PID}" 2>/dev/null; then
  echo "process ${PID} not running removing stale pid file" >&2
  rm -f "${PIDFILE}"
  exit 0
fi

kill "${PID}" 2>/dev/null || true
for _ in {1..30}; do
  if ! kill -0 "${PID}" 2>/dev/null; then
    rm -f "${PIDFILE}"
    echo "stopped ${PID}"
    exit 0
  fi
  sleep 1
done

echo "force kill ${PID}" >&2
kill -9 "${PID}" 2>/dev/null || true
rm -f "${PIDFILE}"
echo "stopped ${PID}"
