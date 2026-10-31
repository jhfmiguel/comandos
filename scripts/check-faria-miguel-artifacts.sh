#!/usr/bin/env bash
set -euo pipefail

POM="${1:-api/pom.xml}"
REPOSITORY_BASE="https://maven.pkg.github.com/jhfmiguel/faria-miguel/com/fariamiguel"

fail() {
  echo "Faria Miguel artifact preflight: $*" >&2
  exit 1
}

[[ -f "$POM" ]] || fail "POM not found: $POM"
[[ -n "${GITHUB_ACTOR:-}" ]] || fail "GITHUB_ACTOR is not available"
[[ -n "${GITHUB_TOKEN:-}" ]] || fail "GITHUB_TOKEN is not available"

version="$(
  sed -n 's:.*<faria-miguel.version>\(.*\)</faria-miguel.version>.*:\1:p' "$POM" |
    head -n 1 |
    tr -d '[:space:]'
)"

[[ -n "$version" ]] || fail "unable to read faria-miguel.version from $POM"

artifacts=(
  faria-miguel-platform
  faria-miguel-platform-migrations
  faria-miguel-tenancy
  faria-miguel-enterprise
  faria-miguel-enterprise-persistence-jpa
  faria-miguel-commerce
)

missing=()

for artifact in "${artifacts[@]}"; do
  url="$REPOSITORY_BASE/$artifact/maven-metadata.xml"
  metadata="$(mktemp)"
  status="$(
    curl       --silent       --show-error       --location       --user "$GITHUB_ACTOR:$GITHUB_TOKEN"       --output "$metadata"       --write-out '%{http_code}'       "$url" || true
  )"

  if [[ "$status" != "200" ]] || ! grep -Fq "<version>$version</version>" "$metadata"; then
    missing+=("$artifact")
  fi

  rm -f "$metadata"
done

if (( ${#missing[@]} > 0 )); then
  echo "Canonical Faria Miguel version '$version' is not fully available from GitHub Packages." >&2
  printf 'Missing/unavailable artifacts:\n' >&2
  printf ' - %s\n' "${missing[@]}" >&2
  exit 1
fi

echo "All canonical Faria Miguel artifacts for version '$version' are available."
