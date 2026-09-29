#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT_DIR/architecture/product-boundary.json"

fail() {
  echo "COMANDOS product boundary violation: $*" >&2
  exit 1
}

test -f "$MANIFEST" || fail "missing architecture/product-boundary.json"
grep -q '"role": "product-consumer"' "$MANIFEST" || fail "product-consumer role is not declared"
grep -q '"sharedFoundation": "jhfmiguel/faria-miguel"' "$MANIFEST" || fail "shared foundation is not pinned to jhfmiguel/faria-miguel"
grep -q '"directProductDependenciesAllowed": false' "$MANIFEST" || fail "direct sibling-product dependencies must remain disabled"
grep -q '"productSourceImportsAllowed": false' "$MANIFEST" || fail "sibling-product source imports must remain disabled"

# Dependency manifests may consume Faria Miguel shared artifacts, but never a sibling product.
for dependency_file in "$ROOT_DIR/api/pom.xml" "$ROOT_DIR/app/package.json"; do
  [[ -f "$dependency_file" ]] || continue
  if grep -nE '(digital[-_.]?experience|com\.fariamiguel\.(digitalexperience|trator|tubarao)|@fariamiguel/(digital-experience|trator|tubarao))' "$dependency_file"; then
    fail "sibling-product dependency declared in ${dependency_file#$ROOT_DIR/}"
  fi
done

if [[ -d "$ROOT_DIR/api/src" ]] && grep -RInE --include='*.java' \
  '^[[:space:]]*import[[:space:]]+com\.fariamiguel\.(digitalexperience|digital_experience|trator|tubarao)([.;])' \
  "$ROOT_DIR/api/src"; then
  fail "Java source imports a sibling product namespace"
fi

if [[ -d "$ROOT_DIR/app" ]] && grep -RInE --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' \
  "(from[[:space:]]+['\"](@fariamiguel/(digital-experience|trator|tubarao)|.*(digital-experience|trator|tubarao)/src)|require\\(['\"](@fariamiguel/(digital-experience|trator|tubarao)))" \
  "$ROOT_DIR/app" --exclude-dir=node_modules --exclude-dir=.next; then
  fail "frontend source imports sibling-product code"
fi

printf 'COMANDOS product boundary validated; shared capabilities remain externalized to faria-miguel.\n'
