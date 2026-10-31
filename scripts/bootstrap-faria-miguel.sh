#!/usr/bin/env bash
set -euo pipefail

FOUNDATION_PATH="${FARIA_MIGUEL_HOME:-$(cd "$(dirname "$0")/../.." && pwd)/faria-miguel}"
POM="$FOUNDATION_PATH/pom.xml"

if [[ ! -f "$POM" ]]; then
  echo "Faria Miguel foundation not found at '$FOUNDATION_PATH'. Set FARIA_MIGUEL_HOME or keep it as a sibling folder." >&2
  exit 1
fi

mvn -f "$POM" -DskipTests install
