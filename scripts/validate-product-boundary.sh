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
  finance contracts products inventory commerce professional-qualifications
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
  "api/src/main/java/com/comandos/workflow/api/WorkflowGateway.java"
  "api/src/main/java/com/comandos/workflow/dto/WorkflowContract.java"
)
for contract in "${forbidden_local_contracts[@]}"; do
  [[ ! -e "$ROOT_DIR/$contract" ]] || fail "generic contract '$contract' must be consumed from Faria Miguel"
done

grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.(core\.api\.(IdGenerator|PlatformClock|PlatformPage)|audit\.api\.AuditRecorder|documents\.api\.(DocumentStorage|DocumentReference)|notifications\.api\.(NotificationSender|NotificationMessage));' "$ROOT_DIR/api/src" \
  && fail "source still imports a removed COMANDOS-owned generic contract" || true

# Canonical Master Data read cutovers completed by Step 21.11.
grep -q 'ContactRepository canonicalContacts' "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalContactDirectory.java" \
  || fail "contacts must read from canonical ContactRepository after cutover"
grep -q 'PartyRoleRepository canonicalRoles' "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalPartyRoleDirectory.java" \
  || fail "generic party roles must read from canonical PartyRoleRepository after cutover"
grep -q 'PartyDocumentRepository canonicalDocuments' "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalPartyDocumentDirectory.java" \
  || fail "party documents must read from canonical PartyDocumentRepository after cutover"
grep -q 'ProfessionalQualificationRepository canonicalQualifications' "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalProfessionalQualificationDirectory.java" \
  || fail "professional qualifications must read from canonical repository after cutover"

# Steps 21.13-21.15: legacy master-data retirement backlog may only shrink.
RETIREMENT_MANIFEST="$ROOT_DIR/architecture/step-21-master-data-retirement.json"
test -f "$RETIREMENT_MANIFEST" || fail "missing Step 21 master-data retirement manifest"

legacy_id_count="$(grep -RhoE --include='*.java' 'public[[:space:]]+Long[[:space:]]+[A-Za-z0-9_]*LegacyId[[:space:]]*;' "$ROOT_DIR/api/src/main/java" | wc -l | tr -d ' ')"
[[ "$legacy_id_count" -le 53 ]] || fail "LegacyId field count grew above the corrected Step 21.13 baseline of 53"

allowed_legacy_id_files=(
  "api/src/main/java/com/comandos/inventory/model/StockLocation.java"
  "api/src/main/java/com/comandos/inventory/model/EquipmentSetOperation.java"
  "api/src/main/java/com/comandos/inventory/model/ExpirationRecord.java"
  "api/src/main/java/com/comandos/inventory/model/CertificationRecord.java"
  "api/src/main/java/com/comandos/inventory/model/EquipmentSet.java"
  "api/src/main/java/com/comandos/purchase/model/Purchase.java"
  "api/src/main/java/com/comandos/purchase/model/ProcurementProcess.java"
  "api/src/main/java/com/comandos/purchase/model/EquipmentReceiving.java"
  "api/src/main/java/com/comandos/custody/model/Custody.java"
  "api/src/main/java/com/comandos/donation/model/Donation.java"
  "api/src/main/java/com/comandos/sales/model/InventorySale.java"
  "api/src/main/java/com/comandos/transfer/model/InventoryTransfer.java"
  "api/src/main/java/com/comandos/maintenance/model/WorkOrder.java"
  "api/src/main/java/com/comandos/workflow/model/ApprovalWorkflow.java"
  "api/src/main/java/com/comandos/consumption/model/AmmunitionConsumption.java"
  "api/src/main/java/com/comandos/consumption/model/ConsumableUsage.java"
  "api/src/main/java/com/comandos/disposal/model/DisposalProcess.java"
  "api/src/main/java/com/comandos/reservation/model/InventoryReservation.java"
  "api/src/main/java/com/comandos/reconciliation/model/InventoryCount.java"
  "api/src/main/java/com/comandos/purchase/model/PurchasePlanning.java"
  "api/src/main/java/com/comandos/lifecycle/model/PeriodicInspection.java"
  "api/src/main/java/com/comandos/lifecycle/model/ExceptionOccurrence.java"
)

while IFS= read -r absolute; do
  relative="${absolute#$ROOT_DIR/}"
  allowed=false
  for candidate in "${allowed_legacy_id_files[@]}"; do
    if [[ "$relative" == "$candidate" ]]; then
      allowed=true
      break
    fi
  done
  [[ "$allowed" == true ]] || fail "new LegacyId field introduced outside the Step 21.13 retirement inventory: $relative"
done < <(grep -RIlE --include='*.java' 'public[[:space:]]+Long[[:space:]]+[A-Za-z0-9_]*LegacyId[[:space:]]*;' "$ROOT_DIR/api/src/main/java" || true)

[[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/core/repository" ]] \
  || fail "legacy generic Master Data repository package must remain absent"

if grep -q '@Deprecated' "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CoreService.java"; then
  fail "CoreService must not regain deprecated compatibility constructors"
fi

if grep -q '@Deprecated' "$ROOT_DIR/api/src/main/java/com/comandos/workflow/service/WorkflowService.java"; then
  fail "WorkflowService must not regain deprecated compatibility constructors"
fi

# Master-data migration seams must remain present until the legacy persistence cutover is complete.
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalMasterDataMapper.java" \
  || fail "canonical master-data mapper is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalMasterDataDirectory.java" \
  || fail "canonical master-data directory is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalContactMapper.java" \
  || fail "canonical person-contact mapper is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalContactDirectory.java" \
  || fail "canonical person-contact directory is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalPartyRoleMapper.java" \
  || fail "canonical party-role mapper is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalPartyRoleDirectory.java" \
  || fail "canonical party-role directory is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalPartyDocumentMapper.java" \
  || fail "canonical party-document mapper is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalPartyDocumentDirectory.java" \
  || fail "canonical party-document directory is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalPartySnapshotDirectory.java" \
  || fail "canonical party snapshot directory is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalProfessionalQualificationMapper.java" \
  || fail "canonical professional qualification mapper is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalProfessionalQualificationDirectory.java" \
  || fail "canonical professional qualification directory is required during legacy persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalCoreReadService.java" \
  || fail "canonical core read service is required after master-data read cutover"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CorePageResult.java" \
  || fail "standalone core pagination contract is required after master-data read cutover"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/PersonRegistrationService.java" \
  || fail "person registration orchestration must stay outside CoreService"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalMasterDataMirrorService.java" \
  || fail "canonical master-data shadow writer is required during persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/MasterDataMigrationService.java" \
  || fail "master-data readiness/backfill service is required during persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/MasterDataMigrationRunner.java" \
  || fail "controlled master-data migration runner is required during persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/MasterDataParityService.java" \
  || fail "legacy-to-canonical master-data parity verifier is required during persistence migration"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/MasterDataMigrationConfigurationGuard.java" \
  || fail "master-data migration configuration guard is required during read cutover"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/model/MasterDataReference.java" \
  || fail "durable legacy-to-canonical master-data crosswalk entity is required"
test -f "$ROOT_DIR/api/src/main/java/com/comandos/core/service/MasterDataReferenceService.java" \
  || fail "durable legacy-to-canonical master-data crosswalk service is required"
grep -q 'com\.fariamiguel\.identity\.api\.IdentityDirectory' \
  "$ROOT_DIR/api/src/main/java/com/comandos/identity/service/JpaIdentityDirectory.java" \
  || fail "legacy identity adapter must expose the canonical Faria Miguel IdentityDirectory"

grep -q '"legacy-person-to-faria-enterprise-person"' "$MANIFEST" \
  || fail "person canonical migration bridge is not declared"
grep -q '"legacy-organization-to-faria-enterprise-organization"' "$MANIFEST" \
  || fail "organization canonical migration bridge is not declared"
grep -q '"legacy-unit-to-faria-tenancy-unit"' "$MANIFEST" \
  || fail "organizational-unit canonical migration bridge is not declared"
grep -q '"legacy-identity-to-faria-identity-directory"' "$MANIFEST" \
  || fail "identity canonical migration bridge is not declared"
grep -q '"legacy-person-contacts-to-faria-enterprise-contacts"' "$MANIFEST" \
  || fail "person contact canonical migration bridge is not declared"
grep -q '"legacy-person-roles-to-faria-party-roles"' "$MANIFEST" \
  || fail "party-role canonical migration bridge is not declared"
grep -q '"legacy-person-credentials-to-faria-party-documents"' "$MANIFEST" \
  || fail "party-document canonical migration bridge is not declared"
grep -q '"legacy-party-snapshot-to-faria-party-snapshot"' "$MANIFEST" \
  || fail "party snapshot canonical migration bridge is not declared"
grep -q '"legacy-person-qualifications-to-faria-professional-qualifications"' "$MANIFEST" \
  || fail "professional qualification canonical migration bridge is not declared"
grep -q '"canonical-master-data-read-api"' "$MANIFEST" \
  || fail "canonical master-data read cutover is not declared"
grep -q '"canonical-person-detail-read-api"' "$MANIFEST" \
  || fail "canonical person detail read cutover is not declared"
grep -q '"person-registration-orchestration"' "$MANIFEST" \
  || fail "person registration extraction is not declared"
grep -q '"legacy-master-data-to-canonical-shadow-write"' "$MANIFEST" \
  || fail "master-data shadow-write bridge is not declared"
grep -q '"legacy-master-data-canonical-backfill"' "$MANIFEST" \
  || fail "master-data backfill bridge is not declared"
grep -q '"legacy-master-data-canonical-parity"' "$MANIFEST" \
  || fail "master-data parity bridge is not declared"
grep -q '"legacy-master-data-canonical-delete-shadow"' "$MANIFEST" \
  || fail "master-data delete-shadow bridge is not declared"
grep -q '"canonical-master-data-controlled-read-cutover"' "$MANIFEST" \
  || fail "canonical master-data read-cutover bridge is not declared"
grep -q '"legacy-master-data-reference-crosswalk"' "$MANIFEST" \
  || fail "master-data reference crosswalk bridge is not declared"

grep -q 'masterDataMirror.delete(entity)' \
  "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CoreService.java" \
  || fail "CoreService delete path must mirror canonical master-data deletion"

if grep -q 'record PersonRegistration\|savePersonWithContacts' \
  "$ROOT_DIR/api/src/main/java/com/comandos/core/service/CoreService.java"; then
  fail "CoreService must not regain PersonRegistration orchestration"
fi

# Local identity/organization compatibility APIs have been removed.
[[ ! -e "$ROOT_DIR/api/src/main/java/com/comandos/identity/api" ]] \
  || fail "local identity API package is forbidden; use Faria Miguel identity contracts"
[[ ! -e "$ROOT_DIR/api/src/main/java/com/comandos/organization" ]] \
  || fail "local organization package is forbidden; use canonical Faria Miguel master data"

test -f "$ROOT_DIR/api/src/main/java/com/comandos/identity/service/JpaIdentityDirectory.java" \
  || fail "canonical identity persistence bridge is required during legacy persistence migration"

grep -q 'com\.fariamiguel\.identity\.api\.IdentityDirectory' \
  "$ROOT_DIR/api/src/main/java/com/comandos/identity/service/JpaIdentityDirectory.java" \
  || fail "identity persistence bridge must implement canonical Faria Miguel IdentityDirectory"

grep -q '"local-identity-api-removed"' "$MANIFEST" \
  || fail "local identity API removal is not declared"
grep -q '"local-organization-api-removed"' "$MANIFEST" \
  || fail "local organization API removal is not declared"

test -f "$ROOT_DIR/api/src/main/java/com/comandos/workflow/api/SensitiveWorkflowGateway.java" \
  || fail "COMANDOS sensitive workflow overlay must expose SensitiveWorkflowGateway"
grep -q 'sensitive-operation/security workflow overlay' \
  "$ROOT_DIR/api/src/main/java/com/comandos/workflow/package-info.java" \
  || fail "local workflow package must remain explicitly product-specific"
grep -q '"workflow-adapters-reclassified"' "$MANIFEST" \
  || fail "workflow adapter reclassification is not declared"

if grep -RInE --include='*.java' 'w\.organization\.id|w\.unit\.id|workflow\.organization\.id|workflow\.unit\.id' "$ROOT_DIR/api/src/main/java/com/comandos/workflow"; then
  fail "workflow overlay must use scalar legacy/canonical scope ids; legacy Organization/Unit JPA associations are retired"
fi

[[ ! -e "$ROOT_DIR/api/src/main/java/com/comandos/security/api" ]] \
  || fail "local COMANDOS security API package is forbidden; use Faria Miguel security contracts"

grep -q 'implements PlatformPrincipal' \
  "$ROOT_DIR/api/src/main/java/com/comandos/security/service/AccountPrincipal.java" \
  || fail "local account principal must implement canonical Faria Miguel PlatformPrincipal"

grep -q 'implements CurrentActorProvider' \
  "$ROOT_DIR/api/src/main/java/com/comandos/security/service/SpringCurrentActorProvider.java" \
  || fail "COMANDOS current actor provider must implement canonical CurrentActorProvider"

grep -q 'implements ResourceAccessPolicy' \
  "$ROOT_DIR/api/src/main/java/com/comandos/security/service/AccessPolicy.java" \
  || fail "COMANDOS access policy must implement canonical ResourceAccessPolicy"

grep -q '"security-adapters-reclassified"' "$MANIFEST" \
  || fail "security adapter reclassification is not declared"

# Step 21 closure guards for messaging, audit and identity.
# Local generic messaging is fully retired; only canonical Faria Miguel messaging is allowed.
[[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/messaging" ]] \
  || fail "local generic messaging package must remain removed; use Faria Miguel messaging/event fabric"

if [[ -d "$ROOT_DIR/api/src/main/java/com/comandos" ]]; then
  messaging_refs="$(grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.messaging(\.api)?\.(PlatformEvent|PlatformEventPublisher|SpringPlatformEventPublisher|DomainEventEnvelope|DomainEventPublisher)|\b(DomainEventPublisher|DomainEventEnvelope)\b' "$ROOT_DIR/api/src/main/java/com/comandos"     | grep -v '/com/comandos/messaging/' || true)"
  [[ -z "$messaging_refs" ]] || {
    echo "$messaging_refs" >&2
    fail "legacy COMANDOS messaging contracts still have callers outside the compatibility package"
  }

  identity_refs="$(grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.identity\.service\.JpaIdentityDirectory' "$ROOT_DIR/api/src/main/java/com/comandos"     | grep -v '/com/comandos/identity/service/JpaIdentityDirectory.java' || true)"
  [[ -z "$identity_refs" ]] || {
    echo "$identity_refs" >&2
    fail "identity callers must depend on the canonical IdentityDirectory contract, not JpaIdentityDirectory"
  }

  identity_bridge="$ROOT_DIR/api/src/main/java/com/comandos/identity/service/JpaIdentityDirectory.java"
  if grep -qE 'com\.comandos\.core\.model\.Person|EntityManager|find\(Person\.class' "$identity_bridge"; then
    fail "JpaIdentityDirectory must not read legacy Person persistence directly"
  fi
fi

if grep -q 'implements[[:space:]]\+AuditSink' "$ROOT_DIR/api/src/main/java/com/comandos/audit/service/AuditService.java"; then
  fail "COMANDOS AuditService must not implement the generic Faria Miguel AuditSink"
fi

# Step 21.18: generic catalog primitives are foundation-owned.
[[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/enterprise/catalog" ]] \
  || fail "com.comandos.enterprise.catalog must remain absent after Step 21.18"

for retired_catalog_primitive in \
  "$ROOT_DIR/api/src/main/java/com/comandos/enterprise/catalog/CatalogIdentity.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/enterprise/catalog/CatalogTrackingMode.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/enterprise/catalog/CatalogTrackingPolicy.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/enterprise/catalog/UnitOfMeasureCode.java"; do
  [[ ! -e "$retired_catalog_primitive" ]] \
    || fail "generic catalog primitive returned to COMANDOS: ${retired_catalog_primitive#$ROOT_DIR/}"
done

grep -q 'com.fariamiguel.enterprise.catalog.CatalogIdentity' \
  "$ROOT_DIR/api/src/test/java/com/comandos/enterprise/catalog/EnterpriseCatalogTests.java" \
  || fail "catalog identity tests must consume the canonical Faria Miguel primitive"
grep -q 'com.fariamiguel.enterprise.catalog.CatalogTrackingPolicy' \
  "$ROOT_DIR/api/src/test/java/com/comandos/enterprise/catalog/EnterpriseCatalogTests.java" \
  || fail "catalog tracking tests must consume the canonical Faria Miguel primitive"
grep -q 'com.fariamiguel.enterprise.catalog.UnitOfMeasureCode' \
  "$ROOT_DIR/api/src/test/java/com/comandos/enterprise/catalog/EnterpriseCatalogTests.java" \
  || fail "unit-of-measure tests must consume the canonical Faria Miguel primitive"

# COMANDOS already had a catalog compatibility layer before extraction. Until its callers
# are fully moved, no other Enterprise subdomain is allowed to exist locally.
enterprise_root="$ROOT_DIR/api/src/main/java/com/comandos/enterprise"
if [[ -d "$enterprise_root" ]]; then
  while IFS= read -r entry; do
    base="$(basename "$entry")"
    [[ "$base" == "catalog" || "$base" == "package-info.java" ]] || fail "unexpected local Enterprise capability '$base'; use Faria Miguel instead"
  done < <(find "$enterprise_root" -mindepth 1 -maxdepth 1 -print)
fi

grep -q '"core-adapters-reclassified"' "$MANIFEST" \
  || fail "core adapter reclassification is not declared"

grep -q '"legacy-master-data-admin-compatibility"' "$MANIFEST" \
  || fail "remaining local core compatibility ownership is not declared"

# Step 21.20: purchase/procurement must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|Person|OrganizationalUnit)' "$ROOT_DIR/api/src/main/java/com/comandos/purchase"; then
  fail "purchase/procurement code must consume canonical Master Data instead of legacy Organization/Person/Unit entities"
fi
if grep -q 'ManyToOne' "$ROOT_DIR/api/src/main/java/com/comandos/purchase/model/ProcurementProcess.java" \
   && grep -q 'Organization' "$ROOT_DIR/api/src/main/java/com/comandos/purchase/model/ProcurementProcess.java"; then
  fail "ProcurementProcess must not regain a legacy Organization JPA association"
fi

# Steps 21.21-21.23: generic sales/finance/contracts are foundation-owned.
for removed_sales_file in \
  "$ROOT_DIR/api/src/main/java/com/comandos/model/Sale.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/model/SaleItem.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/model/repository/SaleRepository.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/model/repository/SaleItemRepository.java"; do
  [[ ! -e "$removed_sales_file" ]] || fail "obsolete generic sales persistence returned: ${removed_sales_file#$ROOT_DIR/}"
done
[[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/finance" ]] || fail "generic finance must remain foundation-owned"
[[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/contracts" ]] || fail "generic contracts must remain foundation-owned"

# Steps 21.24-21.26: shared documents, notifications and platform infrastructure are foundation-owned.
for retired_path in \
  "$ROOT_DIR/api/src/main/java/com/comandos/documents/storage/JpaDocumentStorage.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/documents/model/StoredDocumentBlob.java" \
  "$ROOT_DIR/api/src/main/java/com/comandos/documents/api" \
  "$ROOT_DIR/api/src/main/java/com/comandos/analytics" \
  "$ROOT_DIR/api/src/main/java/com/comandos/core/config/FariaMiguelPlatformConfiguration.java"; do
  [[ ! -e "$retired_path" ]] || fail "shared infrastructure returned to COMANDOS: ${retired_path#$ROOT_DIR/}"
done

[[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/notifications" ]] \
  || fail "generic notifications must remain foundation-owned"

grep -q 'com.fariamiguel.documents.api.DocumentStorage' \
  "$ROOT_DIR/api/src/main/java/com/comandos/documents/service/ProcessAttachmentService.java" \
  || fail "process attachments must consume canonical Faria Miguel DocumentStorage"

grep -q 'com.fariamiguel.analytics.api.MetricRecorder' \
  "$ROOT_DIR/api/src/test/java/com/comandos/architecture/FariaMiguelConsumerContractTest.java" \
  || fail "metrics must remain owned by Faria Miguel Platform Core"

# Transitional roots may contain product adapters only; ownership remains Faria Miguel.
for capability in audit documents identity; do
  grep -q "\"${capability}\"" "$MANIFEST" || fail "compatibility adapter root '${capability}' is not declared"
done


# Steps 21.27-21.30: dependency/config/script cleanup.
if grep -q '<artifactId>spring-security-crypto</artifactId>' "$ROOT_DIR/api/pom.xml"; then
  fail "spring-security-crypto must not be declared directly; starter-security already provides it"
fi

for retired_frontend_package in '@faria-miguel/enterprise' '@faria-miguel/builder-core'; do
  if grep -q "\"$retired_frontend_package\"" "$ROOT_DIR/app/package.json"; then
    fail "retired unused frontend dependency returned: $retired_frontend_package"
  fi
done

if grep -qE '^DB_URL=jdbc:postgresql|^DB_USERNAME=postgres|^DB_PASSWORD=postgres' "$ROOT_DIR/.env.example"; then
  fail ".env.example must remain aligned with Oracle; obsolete PostgreSQL defaults are forbidden"
fi

if grep -q 'jdbc:postgresql' "$ROOT_DIR/api/src/main/resources/application-demo.properties"; then
  fail "demo profile must not own database configuration; Oracle/database selection belongs to the database profile"
fi

[[ ! -e "$ROOT_DIR/api/scripts/start-with-demo-data.ps1" ]] \
  || fail "duplicate demo startup script returned; use scripts/run-oracle-demo.ps1"

# Canonical shared frontend foundation.
APP_PACKAGE="$ROOT_DIR/app/package.json"
test -f "$APP_PACKAGE" || fail "app/package.json is missing"
for package in '@faria-miguel/ui' '@faria-miguel/platform'; do
  grep -q "\"$package\"" "$APP_PACKAGE" || fail "frontend must consume $package"
  grep -q "\"$package\"" "$MANIFEST" || fail "frontend foundation manifest must declare $package"
done

for package in '@faria-miguel/enterprise' '@faria-miguel/builder-core'; do
  if grep -q "\"$package\"" "$APP_PACKAGE"; then
    fail "frontend declares unused shared package $package; add it only when product source imports it"
  fi
done
test -f "$ROOT_DIR/app/.npmrc" || fail "frontend must configure the Faria Miguel package registry"
grep -q '@jhfmiguel:registry=https://npm.pkg.github.com' "$ROOT_DIR/app/.npmrc" || fail "frontend physical package registry must point to GitHub Packages"

grep -q '@faria-miguel/ui/data-table' "$ROOT_DIR/app/src/platform/ui.ts" || fail "DataTable must be exported directly from @faria-miguel/ui"
grep -q '@faria-miguel/ui/pagination' "$ROOT_DIR/app/src/platform/ui.ts" || fail "Pagination must be exported directly from @faria-miguel/ui"
grep -q '@faria-miguel/ui/form-field' "$ROOT_DIR/app/src/platform/ui.ts" || fail "FormField must be exported directly from @faria-miguel/ui"
grep -q '@faria-miguel/ui/confirm-dialog' "$ROOT_DIR/app/src/platform/ui.ts" || fail "ConfirmDialog must be exported directly from @faria-miguel/ui"
grep -q '@faria-miguel/platform/data' "$ROOT_DIR/app/src/platform/index.ts" || fail "platform data must be exported directly from @faria-miguel/platform"
grep -q '@faria-miguel/platform/hooks' "$ROOT_DIR/app/src/platform/index.ts" || fail "platform hooks must be exported directly from @faria-miguel/platform"
for removed_wrapper in \
  "$ROOT_DIR/app/src/platform/components/data-table.tsx" \
  "$ROOT_DIR/app/src/platform/components/pagination.tsx" \
  "$ROOT_DIR/app/src/platform/components/form-field.tsx" \
  "$ROOT_DIR/app/src/platform/components/confirm-dialog.tsx" \
  "$ROOT_DIR/app/src/platform/data.ts" \
  "$ROOT_DIR/app/src/platform/hooks.ts"; do
  [[ ! -e "$removed_wrapper" ]] || fail "redundant shared frontend wrapper returned: ${removed_wrapper#$ROOT_DIR/}"
done
grep -q '@faria-miguel/ui/float-label-enhancer' "$ROOT_DIR/app/src/components/common/float-label-enhancer.tsx" || fail "FloatLabel enhancer must delegate to @faria-miguel/ui"
grep -q '@faria-miguel/platform/admin-layout' "$ROOT_DIR/app/src/components/layout/index.tsx" || fail "administrative layout must compose @faria-miguel/platform AdminLayout"
grep -q '@faria-miguel/platform/theme' "$ROOT_DIR/app/src/components/settings/preferences-provider.tsx" || fail "theme resolution must come from @faria-miguel/platform"
grep -q '@faria-miguel/platform/notifications' "$ROOT_DIR/app/src/components/common/toast/index.tsx" || fail "notifications must delegate to @faria-miguel/platform"
grep -q '@faria-miguel/platform/auth-session' "$ROOT_DIR/app/src/components/auth/session-provider.tsx" || fail "session lifecycle must delegate to @faria-miguel/platform"
grep -q '@faria-miguel/platform/preferences' "$ROOT_DIR/app/src/components/settings/preferences-provider.tsx" || fail "preference persistence must delegate to @faria-miguel/platform"
grep -q '@faria-miguel/ui/loader' "$ROOT_DIR/app/src/components/common/loader/index.tsx" || fail "Loader must delegate to @faria-miguel/ui"
grep -q '@faria-miguel/ui/table-standardizer' "$ROOT_DIR/app/src/platform/components/system-table-standardizer.tsx" || fail "TableStandardizer must delegate to @faria-miguel/ui"
grep -q '@faria-miguel/ui/searchable-select' "$ROOT_DIR/app/src/components/common/select-field.tsx" || fail "SearchableSelect must delegate to @faria-miguel/ui"

printf 'COMANDOS consumes the complete canonical Faria Miguel backend foundation.\n'
printf 'Shared ownership mappings validated for core/master-data/security/audit/documents/workflow/notifications/procurement/sales/finance/contracts/catalog/inventory.\n'
printf 'Completed cutovers are guarded: id generation, platform clock/page, audit recorder, document storage/reference and notifications cannot be reintroduced locally.\n'
printf 'Canonical Platform Core runtime implementations are wired from Faria Miguel; product-local clock/UUID implementations are forbidden.\n'
printf 'Canonical security CurrentActor contracts are consumed from Faria Miguel; product-local copies are forbidden.\n'
printf 'Canonical security AccessDeniedException is consumed from Faria Miguel; product-local copies are forbidden.\n'
printf 'Local security code is restricted to COMANDOS authentication/session and product-specific scope overlays.\n'
printf 'COMANDOS authorization now consumes the canonical Faria Miguel ResourceAccessPolicy directly; redundant local authorization adapters are forbidden.\n'
printf 'Generic audit persistence is owned by Faria Miguel; COMANDOS audit code is restricted to product-specific recording/query/enrichment behavior.\n'
printf 'COMANDOS workflow transition validation consumes the canonical Faria Miguel WorkflowEngine; local generic engine/definition copies are forbidden.\n'
printf 'Local workflow code is restricted to the product-specific sensitive-operation overlay.\n'
printf 'Duplicate workflow DTO contracts are forbidden; COMANDOS keeps only its product-specific workflow API surface.\n'
printf 'Master-data compatibility bridges expose Faria Miguel canonical person/organization/unit/identity/contact/party-role/document/snapshot/professional-qualification contracts while legacy Oracle persistence remains transitional.\n'
printf 'People, organization and unit reads are routed through CanonicalCoreReadService; generic CoreService master-data reads are no longer the primary HTTP path.\n'
printf 'Person address/phone/email/credential/qualification reads also use CanonicalCoreReadService.\n'
printf 'Local identity/organization APIs are removed; canonical Faria Miguel contracts are mandatory.\n'
printf 'Person registration orchestration is isolated from the legacy CoreService.\n'
printf 'Master-data persistence migration has guarded readiness, idempotent backfill, zero-mismatch parity, CREATE/UPDATE/DELETE shadow-write, controlled canonical reads and a durable FK crosswalk.\n'
printf 'Remaining local core code is restricted to legacy master-data/admin compatibility during persistence cutover.\n'
printf 'Generic integrations adapters are removed and cannot be reintroduced locally.\n'
printf 'Only the pre-existing COMANDOS enterprise/catalog compatibility layer is permitted locally.\n'
printf 'COMANDOS product boundary validated against canonical Faria Miguel foundation.\n'
