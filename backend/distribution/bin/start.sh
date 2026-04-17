#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="${ROOT}/lib/ass-kicker-svr.jar"
RUN="${ROOT}/run"
LOG="${ROOT}/logs"
mkdir -p "${RUN}" "${LOG}"
PIDFILE="${RUN}/ass-kicker.pid"
OUT="${LOG}/ass-kicker.out"

if [[ -f "${PIDFILE}" ]]; then
  OLD="$(cat "${PIDFILE}")"
  if kill -0 "${OLD}" 2>/dev/null; then
    echo "already running pid ${OLD}" >&2
    exit 1
  fi
  rm -f "${PIDFILE}"
fi

nohup java ${JAVA_OPTS:-} -jar "${JAR}" \
  --spring.config.additional-location="optional:file:${ROOT}/conf/" \
  "$@" >>"${OUT}" 2>&1 &
echo $! >"${PIDFILE}"
echo "started pid $(cat "${PIDFILE}") log ${OUT}"
