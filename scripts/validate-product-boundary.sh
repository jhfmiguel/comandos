#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT_DIR/architecture/product-boundary.json"
POM="$ROOT_DIR/api/pom.xml"
fail(){ echo "COMANDOS product boundary violation: $*" >&2; exit 1; }

test -f "$MANIFEST" || fail "missing product boundary manifest"
grep -q '"sharedFoundation": "jhfmiguel/faria-miguel"' "$MANIFEST" || fail "shared foundation is not declared"
grep -q '"sourceCopyingAllowed": false' "$MANIFEST" || fail "shared source copying must remain disabled"
grep -q '"directProductDependenciesAllowed": false' "$MANIFEST" || fail "direct sibling dependencies must remain disabled"

test -f "$POM" || fail "api/pom.xml is missing"
grep -q '<artifactId>faria-miguel-platform</artifactId>' "$POM" || fail "backend must consume faria-miguel-platform"
grep -q '<artifactId>faria-miguel-enterprise</artifactId>' "$POM" || fail "backend must consume faria-miguel-enterprise"

for f in "$POM" "$ROOT_DIR/app/package.json"; do
  [[ -f "$f" ]] || continue
  if grep -nE '(com\.trator|com\.tubarao|digital[-_.]?experience|com\.arnold|@fariamiguel/(trator|tubarao|digital-experience|arnold))' "$f"; then
    fail "sibling-product dependency declared in ${f#$ROOT_DIR/}"
  fi
done

if [[ -d "$ROOT_DIR/api/src" ]] && grep -RInE --include='*.java' '^[[:space:]]*import[[:space:]]+(com\.trator|com\.tubarao|com\.arnold|com\.fariamiguel\.digitalexperience)([.;])' "$ROOT_DIR/api/src"; then
  fail "Java source imports sibling-product code"
fi

# Existing roots below are migration debt only. New generic capabilities must start in Faria Miguel.
for shared in enterprise tenancy productcontrol builders commerce; do
  [[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/$shared" ]] || fail "shared capability '$shared' must live in jhfmiguel/faria-miguel"
done

printf 'COMANDOS shared legacy runtime allowed only for migration: core/audit/documents/workflow/notifications/integrations/security.\n'
printf 'COMANDOS product boundary validated.\n'
