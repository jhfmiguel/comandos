#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT_DIR/architecture/product-boundary.json"
POM="$ROOT_DIR/api/pom.xml"
fail(){ echo "COMANDOS product boundary violation: $*" >&2; exit 1; }

test -f "$MANIFEST" || fail "missing product boundary manifest"
grep -q '"role": "product-consumer"' "$MANIFEST" || fail "role must remain product-consumer"
grep -q '"sharedFoundation": "jhfmiguel/faria-miguel"' "$MANIFEST" || fail "shared foundation is not declared"
grep -q '"sharedCapabilityOwnership": "faria-miguel"' "$MANIFEST" || fail "shared capability ownership must remain faria-miguel"
grep -q '"sourceCopyingAllowed": false' "$MANIFEST" || fail "shared source copying must remain disabled"
grep -q '"productSourceImportsAllowed": false' "$MANIFEST" || fail "product source imports must remain disabled"
grep -q '"directProductDependenciesAllowed": false' "$MANIFEST" || fail "direct sibling dependencies must remain disabled"

test -f "$POM" || fail "api/pom.xml is missing"
grep -q '<faria-miguel.version>0.1.0-SNAPSHOT</faria-miguel.version>' "$POM" || fail "backend must pin the canonical Faria Miguel version"

required_artifacts=(
  faria-miguel-platform
  faria-miguel-platform-migrations
  faria-miguel-tenancy
  faria-miguel-enterprise
  faria-miguel-enterprise-persistence-jpa
  faria-miguel-commerce
)
for artifact in "${required_artifacts[@]}"; do
  grep -q "<artifactId>${artifact}</artifactId>" "$POM" || fail "backend must consume ${artifact}"
  grep -q "\"com.fariamiguel:${artifact}\"" "$MANIFEST" || fail "product boundary must declare ${artifact}"
done

required_capabilities=(
  core people organizations organizational-units contacts customers suppliers
  security audit documents workflow notifications persistence purchases sales
  finance contracts products inventory commerce
)
for capability in "${required_capabilities[@]}"; do
  grep -q "\"${capability}\"" "$MANIFEST" || fail "missing shared ownership mapping for ${capability}"
done

for f in "$POM" "$ROOT_DIR/app/package.json"; do
  [[ -f "$f" ]] || continue
  if grep -nE '(com\.trator|com\.tubarao|digital[-_.]?experience|com\.arnold|@(?:trator|tubarao|arnold)/|@fariamiguel/(trator|tubarao|digital-experience|arnold))' "$f"; then
    fail "sibling-product dependency declared in ${f#$ROOT_DIR/}"
  fi
done

if [[ -d "$ROOT_DIR/api/src" ]] && grep -RInE --include='*.java' '^[[:space:]]*(package|import)[[:space:]]+(com\.trator|com\.tubarao|com\.arnold|com\.fariamiguel\.(digitalexperience|digital_experience|trator|tubarao|arnold))([.;])' "$ROOT_DIR/api/src"; then
  fail "Java source references sibling-product code"
fi

if [[ -d "$ROOT_DIR/app" ]] && grep -RInE --exclude-dir=node_modules --exclude-dir=.next --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' "(from[[:space:]]+['\"](@(?:trator|tubarao|arnold)/|@fariamiguel/(trator|tubarao|digital-experience|arnold)|.*(trator|tubarao|digital-experience|arnold)/src)|require\\(['\"](@(?:trator|tubarao|arnold)/|@fariamiguel/(trator|tubarao|digital-experience|arnold)))" "$ROOT_DIR/app"; then
  fail "frontend source imports sibling-product code"
fi

# Shared platform/domain modules cannot be reintroduced as new COMANDOS-owned roots.
for shared in enterprise tenancy productcontrol builders commerce; do
  [[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/$shared" ]] || fail "shared capability '$shared' must live in jhfmiguel/faria-miguel"
done

# Product-owned domains remain in COMANDOS; generic behavior in these roots must be expressed
# through Faria Miguel contracts and only security-specific overlays may remain locally.
for capability in core audit documents workflow notifications integrations security; do
  grep -q "\"${capability}\"" "$MANIFEST" || fail "compatibility adapter root '${capability}' is not declared"
done

printf 'COMANDOS consumes the complete canonical Faria Miguel backend foundation.\n'
printf 'Shared ownership mappings validated for core/master-data/security/audit/documents/workflow/notifications/procurement/sales/finance/contracts/catalog/inventory.\n'
printf 'COMANDOS product boundary validated against canonical Faria Miguel foundation.\n'
