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
  if grep -nE '(com\.trator|com\.tubarao|digital[-_.]?experience|com\.arnold|@(trator|tubarao|arnold)/|@fariamiguel/(trator|tubarao|digital-experience|arnold))' "$f"; then
    fail "sibling-product dependency declared in ${f#$ROOT_DIR/}"
  fi
done

if [[ -d "$ROOT_DIR/api/src" ]] && grep -RInE --include='*.java' '^[[:space:]]*(package|import)[[:space:]]+(com\.trator|com\.tubarao|com\.arnold|com\.fariamiguel\.(digitalexperience|digital_experience|trator|tubarao|arnold))([.;])' "$ROOT_DIR/api/src"; then
  fail "Java source references sibling-product code"
fi

if [[ -d "$ROOT_DIR/app" ]] && grep -RInE --exclude-dir=node_modules --exclude-dir=.next --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' "(from[[:space:]]+['\"](@(trator|tubarao|arnold)/|@fariamiguel/(trator|tubarao|digital-experience|arnold)|.*(trator|tubarao|digital-experience|arnold)/src)|require\\(['\"](@(trator|tubarao|arnold)/|@fariamiguel/(trator|tubarao|digital-experience|arnold)))" "$ROOT_DIR/app"; then
  fail "frontend source imports sibling-product code"
fi

# Shared platform/domain modules that have completed cutover cannot be reintroduced as COMANDOS-owned roots.
for shared in tenancy productcontrol builders commerce notifications; do
  [[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/$shared" ]] || fail "shared capability '$shared' must live in jhfmiguel/faria-miguel"
done

# Generic contracts already cut over to Faria Miguel must never be recreated locally.
forbidden_local_contracts=(
  "api/src/main/java/com/comandos/core/api/IdGenerator.java"
  "api/src/main/java/com/comandos/core/api/PlatformClock.java"
  "api/src/main/java/com/comandos/core/api/PlatformPage.java"
  "api/src/main/java/com/comandos/core/service/SystemPlatformClock.java"
  "api/src/main/java/com/comandos/core/service/UuidGenerator.java"
  "api/src/main/java/com/comandos/audit/api/AuditRecorder.java"
  "api/src/main/java/com/comandos/documents/api/DocumentStorage.java"
  "api/src/main/java/com/comandos/documents/api/DocumentReference.java"
  "api/src/main/java/com/comandos/notifications/api/NotificationSender.java"
  "api/src/main/java/com/comandos/notifications/api/NotificationMessage.java"
  "api/src/main/java/com/comandos/security/api/CurrentActor.java"
  "api/src/main/java/com/comandos/security/api/CurrentActorProvider.java"
  "api/src/main/java/com/comandos/security/api/AccessDeniedException.java"
  "api/src/main/java/com/comandos/security/api/AuthorizationService.java"
  "api/src/main/java/com/comandos/security/service/AccessPolicyAuthorizationService.java"
  "api/src/main/java/com/comandos/security/service/FariaMiguelResourceAccessPolicyAdapter.java"
  "api/src/main/java/com/comandos/audit/service/FariaMiguelAuditSinkAdapter.java"
  "api/src/main/java/com/comandos/workflow/api/WorkflowEngine.java"
  "api/src/main/java/com/comandos/workflow/api/WorkflowDefinition.java"
  "api/src/main/java/com/comandos/workflow/dto/WorkflowContract.java"
)
for contract in "${forbidden_local_contracts[@]}"; do
  [[ ! -e "$ROOT_DIR/$contract" ]] || fail "generic contract '$contract' must be consumed from Faria Miguel"
done

grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.(core\.api\.(IdGenerator|PlatformClock|PlatformPage)|audit\.api\.AuditRecorder|documents\.api\.(DocumentStorage|DocumentReference)|notifications\.api\.(NotificationSender|NotificationMessage));' "$ROOT_DIR/api/src" \
  && fail "source still imports a removed COMANDOS-owned generic contract" || true

# COMANDOS already had a catalog compatibility layer before extraction. Until its callers
# are fully moved, no other Enterprise subdomain is allowed to exist locally.
enterprise_root="$ROOT_DIR/api/src/main/java/com/comandos/enterprise"
if [[ -d "$enterprise_root" ]]; then
  while IFS= read -r entry; do
    base="$(basename "$entry")"
    [[ "$base" == "catalog" || "$base" == "package-info.java" ]] || fail "unexpected local Enterprise capability '$base'; use Faria Miguel instead"
  done < <(find "$enterprise_root" -mindepth 1 -maxdepth 1 -print)
fi

# Transitional roots may contain product adapters only; ownership remains Faria Miguel.
for capability in core audit documents workflow integrations security; do
  grep -q "\"${capability}\"" "$MANIFEST" || fail "compatibility adapter root '${capability}' is not declared"
done

printf 'COMANDOS consumes the complete canonical Faria Miguel backend foundation.\n'
printf 'Shared ownership mappings validated for core/master-data/security/audit/documents/workflow/notifications/procurement/sales/finance/contracts/catalog/inventory.\n'
printf 'Completed cutovers are guarded: id generation, platform clock/page, audit recorder, document storage/reference and notifications cannot be reintroduced locally.\n'
printf 'Canonical Platform Core runtime implementations are wired from Faria Miguel; product-local clock/UUID implementations are forbidden.\n'
printf 'Canonical security CurrentActor contracts are consumed from Faria Miguel; product-local copies are forbidden.\n'
printf 'Canonical security AccessDeniedException is consumed from Faria Miguel; product-local copies are forbidden.\n'
printf 'COMANDOS authorization now consumes the canonical Faria Miguel ResourceAccessPolicy directly; redundant local authorization adapters are forbidden.\n'
printf 'COMANDOS audit service implements the canonical Faria Miguel AuditSink directly; redundant audit adapters are forbidden.\n'
printf 'COMANDOS workflow transition validation consumes the canonical Faria Miguel WorkflowEngine; local generic engine/definition copies are forbidden.\n'
printf 'Duplicate workflow DTO contracts are forbidden; COMANDOS keeps only its product-specific workflow API surface.\n'
printf 'Only the pre-existing COMANDOS enterprise/catalog compatibility layer is permitted locally.\n'
printf 'COMANDOS product boundary validated against canonical Faria Miguel foundation.\n'
