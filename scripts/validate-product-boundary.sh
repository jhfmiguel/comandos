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

# Step 21.15: canonical Core reads must not keep a legacy fallback constructor.
CANONICAL_CORE_READS="$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalCoreReadService.java"
if grep -q '@Deprecated' "$CANONICAL_CORE_READS" \
   || grep -q 'this\.masterData = null' "$CANONICAL_CORE_READS" \
   || grep -q 'masterData[[:space:]]*==[[:space:]]*null' "$CANONICAL_CORE_READS" \
   || grep -q 'CanonicalMasterDataMapper\.' "$CANONICAL_CORE_READS"; then
  fail "CanonicalCoreReadService must not keep legacy Master Data read fallbacks"
fi
grep -q 'CanonicalMasterDataDirectory masterData' "$CANONICAL_CORE_READS" \
  || fail "CanonicalCoreReadService must depend on CanonicalMasterDataDirectory"

# Step 21.15/21.32: product services must not directly read legacy Master Data entities.
legacy_master_data_read_hits="$(
  grep -RnE --include='*.java' \
    'find\((Person|Organization|OrganizationalUnit)\.class|locked\((Person|Organization|OrganizationalUnit)\.class|from[[:space:]]+(Person|Organization|OrganizationalUnit)[[:space:]]' \
    "$ROOT_DIR/api/src/main/java/com/comandos" 2>/dev/null \
    | grep -v '/core/service/' \
    | grep -v '/core/model/' \
    | grep -v '/demo/' \
    || true
)"
if [[ -n "$legacy_master_data_read_hits" ]]; then
  printf '%s\n' "$legacy_master_data_read_hits"
  fail "product code still reads legacy Master Data entities directly"
fi

if grep -nE 'location\.(organization|unit)\.' "$ROOT_DIR/api/src/main/java/com/comandos/compliance/service/ComplianceService.java"; then
  fail "ComplianceService must use scalar/canonical StockLocation scope fields; only CompliancePolicy organization/unit remains transitional"
fi

stock_location_navigation_hits="$(
  grep -RInE --include='*.java' '\.location\.(organization|unit)(\.|[^A-Za-z0-9_]|$)'     "$ROOT_DIR/api/src/main/java/com/comandos" 2>/dev/null     | grep -v '/core/'     | grep -v '/demo/'     || true
)"
if [[ -n "$stock_location_navigation_hits" ]]; then
  printf '%s\n' "$stock_location_navigation_hits"
  fail "production code still navigates retired StockLocation organization/unit associations"
fi

legacy_scope_navigation_hits="$(
  grep -RInE --include='*.java' '\.(organization|unit)\.id([^A-Za-z0-9_]|$)'     "$ROOT_DIR/api/src/main/java/com/comandos" 2>/dev/null     | grep -v '/core/'     | grep -v '/compliance/'     | grep -v '/demo/'     | grep -v '/security/service/AccessPolicy.java'     || true
)"
if [[ -n "$legacy_scope_navigation_hits" ]]; then
  printf '%s\n' "$legacy_scope_navigation_hits"
  fail "product code still navigates legacy organization/unit entity associations"
fi

unit_scope_guard_hits="$(
  grep -RInE --include='*UnitScopeGuard.java' '\.(organization|unit|sourceUnit|destinationUnit|recipientUnit)\.id([^A-Za-z0-9_]|$)'     "$ROOT_DIR/api/src/main/java/com/comandos" 2>/dev/null     | grep -v '/core/service/UnitScopeGuard.java'     || true
)"
if [[ -n "$unit_scope_guard_hits" ]]; then
  printf '%s\n' "$unit_scope_guard_hits"
  fail "UnitScopeGuard implementation regressed to legacy Master Data association navigation"
fi

legacy_party_navigation_hits="$(
  grep -RInE --include='*.java' '\.(buyerOrganization|supplierOrganization|originPerson|receivingOrganization|recipient|recipientUnit|authorizer|donor|donee|buyer|sourceUnit|destinationUnit|responsible|responsiblePerson|roleAssignment)\.id([^A-Za-z0-9_]|$)'     "$ROOT_DIR/api/src/main/java/com/comandos" 2>/dev/null     | grep -v '/core/'     | grep -v '/compliance/'     | grep -v '/demo/'     | grep -v '/security/service/AccessPolicy.java'     || true
)"
if [[ -n "$legacy_party_navigation_hits" ]]; then
  printf '%s\n' "$legacy_party_navigation_hits"
  fail "product code still navigates retired Master Data person/party/unit associations"
fi

legacy_master_data_object_navigation_hits="$(
  grep -RInE --include='*.java' '\.(organization|unit|buyerOrganization|supplierOrganization|originPerson|receivingOrganization|recipient|recipientUnit|authorizer|donor|donee|buyer|sourceUnit|destinationUnit|responsible|responsiblePerson|roleAssignment)\.'     "$ROOT_DIR/api/src/main/java/com/comandos" 2>/dev/null     | grep -v '/core/'     | grep -v '/compliance/'     | grep -v '/demo/'     | grep -v '/security/service/AccessPolicy.java'     | grep -v 'com\.fariamiguel\.enterprise\.organization\.Organization'     | grep -v 'com\.fariamiguel\.tenancy\.api\.OrganizationalUnit'     || true
)"
if [[ -n "$legacy_master_data_object_navigation_hits" ]]; then
  printf '%s\n' "$legacy_master_data_object_navigation_hits"
  fail "product code still navigates retired Master Data objects"
fi

# Step 21.36: custody responsibility bridge is frozen until cutover.
CUSTODY_RESPONSIBILITY="$ROOT_DIR/api/src/main/java/com/comandos/custody/model/CustodyResponsibility.java"
CUSTODY_ISSUE_FACADE="$ROOT_DIR/api/src/main/java/com/comandos/custody/service/CustodyIssueFacade.java"
custody_legacy_id_count="$(
  grep -cE 'public Long (responsiblePersonLegacyId|roleAssignmentLegacyId);' "$CUSTODY_RESPONSIBILITY" 2>/dev/null || true
)"
[[ "$custody_legacy_id_count" == "2" ]]   || fail "CustodyResponsibility compatibility bridge changed; expected exactly two approved legacy identifiers"
grep -q 'public String responsiblePersonCanonicalId;' "$CUSTODY_RESPONSIBILITY"   || fail "CustodyResponsibility must retain the canonical responsible person identifier during cutover"
grep -q 'CanonicalPartyRoleDirectory partyRoles' "$CUSTODY_ISSUE_FACADE"   || fail "CustodyIssueFacade must validate responsibility through CanonicalPartyRoleDirectory"
grep -q 'CanonicalMasterDataDirectory masterData' "$CUSTODY_ISSUE_FACADE"   || fail "CustodyIssueFacade must validate responsibility through CanonicalMasterDataDirectory"
grep -q 'canonicalScope.person(responsible.id())' "$CUSTODY_ISSUE_FACADE"   || fail "CustodyIssueFacade must persist the canonical responsible person reference"

# Step 21.36: CoreAccessDemoSeeder may keep only the enumerated security demo compatibility references until cutover.
CORE_ACCESS_DEMO="$ROOT_DIR/api/src/main/java/com/comandos/demo/CoreAccessDemoSeeder.java"
core_access_demo_hits="$(
  grep -nE '\b(Person|Organization|OrganizationalUnit)\b|\.person[[:space:]]*=|\.organization[[:space:]]*=|\.unit[[:space:]]*=' "$CORE_ACCESS_DEMO" 2>/dev/null || true
)"
core_access_demo_count="$(
  printf '%s\n' "$core_access_demo_hits" | sed '/^$/d' | wc -l | tr -d ' '
)"
[[ "$core_access_demo_count" == "8" ]]   || fail "CoreAccessDemoSeeder compatibility surface changed; expected exactly 8 legacy Master Data references"
grep -q 'Person person = one(Person.class, "taxId", "22222222222");' "$CORE_ACCESS_DEMO"   || fail "CoreAccessDemoSeeder person compatibility seed changed unexpectedly"
grep -q 'Organization org = one(Organization.class, "acronym", "SSP-DEMO");' "$CORE_ACCESS_DEMO"   || fail "CoreAccessDemoSeeder organization compatibility seed changed unexpectedly"
grep -q 'OrganizationalUnit unit = one(OrganizationalUnit.class, "code", "ARM-CENTRAL");' "$CORE_ACCESS_DEMO"   || fail "CoreAccessDemoSeeder unit compatibility seed changed unexpectedly"

# Step 21.15: AccessPolicy may keep only the enumerated security-overlay Master Data paths until cutover.
ACCESS_POLICY="$ROOT_DIR/api/src/main/java/com/comandos/security/service/AccessPolicy.java"
access_policy_legacy_paths="$(
  grep -nE 'organization\.id|unit\.id|personRole\.organization\.id|personRole\.unit\.id' "$ACCESS_POLICY" 2>/dev/null || true
)"
access_policy_expected_count="$(
  printf '%s\n' "$access_policy_legacy_paths" | sed '/^$/d' | wc -l | tr -d ' '
)"
[[ "$access_policy_expected_count" == "4" ]]   || fail "AccessPolicy security compatibility exception changed; expected exactly 4 legacy scope path occurrences"
grep -q 'u\.organization\.id, unit\.id' "$ACCESS_POLICY"   || fail "AccessPolicy grant query security compatibility path changed unexpectedly"
grep -q 'case "core/units" -> new Scope("organization.id", "id");' "$ACCESS_POLICY"   || fail "AccessPolicy core/units security compatibility scope changed unexpectedly"
grep -q 'case "core/person-roles" -> new Scope("organization.id", "unit.id");' "$ACCESS_POLICY"   || fail "AccessPolicy core/person-roles security compatibility scope changed unexpectedly"
grep -q 'case "core/role-data" -> new Scope("personRole.organization.id", "personRole.unit.id");' "$ACCESS_POLICY"   || fail "AccessPolicy core/role-data security compatibility scope changed unexpectedly"

# Step 21.15: product constructors must not inject null into canonical cutover collaborators.
null_constructor_hits="$(
  node - "$ROOT_DIR/api/src/main/java/com/comandos" <<'NODE'
const fs = require('fs');
const path = require('path');
const root = process.argv[2];
const canonicalSignals = /(CanonicalMasterData|ProductCanonicalScope|MasterDataReference|ProductMasterDataReference|InventoryLedger|StockLocationRepository)/;
function walk(dir) {
  for (const entry of fs.readdirSync(dir, {withFileTypes:true})) {
    const file = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(file);
    else if (entry.name.endsWith('.java')) {
      const relative = file.replaceAll('\\', '/').split('/api/src/main/java/')[1];
      if (!relative
          || relative.includes('/core/')
          || relative.includes('/demo/')
          || relative.includes('/compliance/')
          || relative.endsWith('/security/service/AccessPolicy.java')) continue;
      const src = fs.readFileSync(file, 'utf8');
      if (!canonicalSignals.test(src)) continue;
      for (const match of src.matchAll(/this\([\s\S]{0,500}?\);/g)) {
        if (/\bnull\b/.test(match[0])) {
          process.stdout.write(relative + ': constructor delegates null into canonical-capable service\n');
          break;
        }
      }
    }
  }
}
walk(root);
NODE
)"
if [[ -n "$null_constructor_hits" ]]; then
  printf '%s\n' "$null_constructor_hits"
  fail "product compatibility constructor still injects null into canonical collaborators"
fi

# Step 21.12-21.17: destructive retirement must remain blocked until runtime cutover PASS.
RETIREMENT_MANIFEST="$ROOT_DIR/architecture/step-21-master-data-retirement.json"
DUPLICATE_RETIREMENT_MANIFEST="$ROOT_DIR/architecture/step-21-duplicate-retirement.json"
node - "$RETIREMENT_MANIFEST" "$DUPLICATE_RETIREMENT_MANIFEST" <<'NODE'
const fs = require('fs');
const retirement = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const duplicate = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'));

if (retirement.blocker?.step !== '21.12') {
  throw new Error('Master Data retirement blocker must remain Step 21.12 until runtime cutover PASS');
}
if (retirement.postCutoverRemovalPlan?.status !== 'prepared-blocked-by-21.12') {
  throw new Error('Post-cutover removal plan must remain blocked by Step 21.12 before runtime PASS');
}
const expectedOrder = ['21.13','21.14','21.15','21.16','21.17'];
const actualOrder = duplicate.postCutoverSequence?.order || [];
if (JSON.stringify(actualOrder) !== JSON.stringify(expectedOrder)) {
  throw new Error('Post-cutover retirement order changed unexpectedly');
}
for (const group of duplicate.groups || []) {
  if (['master-data-persistence-bridges','legacy-master-data-code'].includes(group.id)
      && group.safeToDelete === true) {
    throw new Error(group.id + ' cannot be safeToDelete before Step 21.12 PASS');
  }
}
NODE

# Step 21.15/21.36: production Java must not reintroduce deprecated compatibility APIs.
deprecated_hits="$(
  grep -RFn --include='*.java' '@Deprecated' "$ROOT_DIR/api/src/main/java" 2>/dev/null || true
)"
if [[ -n "$deprecated_hits" ]]; then
  printf '%s\n' "$deprecated_hits"
  fail "deprecated production compatibility API returned"
fi

# Step 21.15: required canonical collaborators are never optional after constructor migration.
for nullable_collaborator in canonicalScope masterDataReferences masterDataMirror canonicalReferences canonicalInventoryLedger canonicalStockLocations; do
  nullable_hits="$(
    grep -RnE --include='*.java' "$nullable_collaborator[[:space:]]*!=[[:space:]]*null|$nullable_collaborator[[:space:]]*==[[:space:]]*null" \
      "$ROOT_DIR/api/src/main/java" 2>/dev/null || true
  )"
  if [[ -n "$nullable_hits" ]]; then
    printf '%s\n' "$nullable_hits"
    fail "required canonical collaborator still has nullable fallback: $nullable_collaborator"
  fi
done

for nullable_collaborator in canonicalScope masterDataReferences masterDataMirror canonicalReferences canonicalInventoryLedger canonicalStockLocations; do
  alternative_nullable_hits="$(
    grep -RnE --include='*.java'       "Optional\.ofNullable\([[:space:]]*$nullable_collaborator|Objects\.(isNull|nonNull)\([[:space:]]*$nullable_collaborator"       "$ROOT_DIR/api/src/main/java" 2>/dev/null || true
  )"
  if [[ -n "$alternative_nullable_hits" ]]; then
    printf '%s\n' "$alternative_nullable_hits"
    fail "required canonical collaborator still has Optional/Objects nullable fallback: $nullable_collaborator"
  fi
done

# Step 21.15: ComplianceService validates organization/unit through canonical Master Data.
COMPLIANCE_SERVICE="$ROOT_DIR/api/src/main/java/com/comandos/compliance/service/ComplianceService.java"
if grep -qE 'em\.find\((Organization|OrganizationalUnit)\.class' "$COMPLIANCE_SERVICE"; then
  fail "ComplianceService must not directly read legacy Organization/OrganizationalUnit"
fi
grep -q 'CanonicalMasterDataDirectory masterData' "$COMPLIANCE_SERVICE"   || fail "ComplianceService must depend on CanonicalMasterDataDirectory for scope validation"

# Step 21.15: InventoryService canonical collaborators are mandatory.
INVENTORY_SERVICE="$ROOT_DIR/api/src/main/java/com/comandos/inventory/service/InventoryService.java"
if grep -qE 'canonicalReferences[[:space:]]*==[[:space:]]*null|masterData[[:space:]]*==[[:space:]]*null|canonicalInventoryLedger[[:space:]]*==[[:space:]]*null|canonicalStockLocations[[:space:]]*==[[:space:]]*null' "$INVENTORY_SERVICE"; then
  fail "InventoryService must not keep nullable canonical collaborator fallbacks"
fi
if grep -q 'ProductMasterDataReferenceSynchronizer masterDataReferences)[[:space:]]*{' "$INVENTORY_SERVICE"; then
  fail "InventoryService must not regain the reduced compatibility constructor"
fi

# Step 21.15: CanonicalMasterDataDirectory must not keep test-only null constructors.
CANONICAL_MASTER_DATA_DIRECTORY="$ROOT_DIR/api/src/main/java/com/comandos/core/service/CanonicalMasterDataDirectory.java"
if grep -q '@Deprecated' "$CANONICAL_MASTER_DATA_DIRECTORY" \
   || grep -q 'this\.canonicalPeople = null' "$CANONICAL_MASTER_DATA_DIRECTORY" \
   || grep -q 'this\.canonicalOrganizations = null' "$CANONICAL_MASTER_DATA_DIRECTORY" \
   || grep -q 'this\.canonicalUnits = null' "$CANONICAL_MASTER_DATA_DIRECTORY"; then
  fail "CanonicalMasterDataDirectory must not keep deprecated null-repository constructors"
fi

# Step 21.15: CoreService master-data mirror is a required collaborator.
CORE_SERVICE="$ROOT_DIR/api/src/main/java/com/comandos/core/service/CoreService.java"
if grep -q 'masterDataMirror[[:space:]]*!=[[:space:]]*null' "$CORE_SERVICE"; then
  fail "CoreService must not keep nullable CanonicalMasterDataMirrorService fallbacks"
fi

# Step 21.15: required workflow cutover collaborators must not be nullable fallbacks.
WORKFLOW_SERVICE="$ROOT_DIR/api/src/main/java/com/comandos/workflow/service/WorkflowService.java"
if grep -q 'masterDataReferences[[:space:]]*!=[[:space:]]*null' "$WORKFLOW_SERVICE" \
   || grep -q 'canonicalScope[[:space:]]*!=[[:space:]]*null' "$WORKFLOW_SERVICE"; then
  fail "WorkflowService must not keep nullable cutover collaborator fallbacks"
fi

# Step 21.36: explicit COMANDOS imports must resolve to a real top-level source file.
# Nested-class imports are valid when one of their enclosing type prefixes exists.
while IFS= read -r import_line; do
  imported="${import_line#import }"
  imported="${imported%;}"
  [[ "$imported" == *".*" ]] && continue

  candidate="$imported"
  resolved=false
  while [[ "$candidate" == com.comandos.* ]]; do
    source_path="$ROOT_DIR/api/src/main/java/${candidate//./\/}.java"
    if [[ -f "$source_path" ]]; then
      resolved=true
      break
    fi
    next="${candidate%.*}"
    [[ "$next" != "$candidate" ]] || break
    candidate="$next"
  done

  if [[ "$resolved" != true ]]; then
    grep -RFn --include='*.java' "import $imported;" "$ROOT_DIR/api/src" 2>/dev/null || true
    fail "orphan COMANDOS import points to a missing source file: $imported"
  fi
done < <(
  grep -RhoE --include='*.java' '^import[[:space:]]+com\.comandos\.[A-Za-z0-9_.]+;' "$ROOT_DIR/api/src" \
    | sort -u
)

# Step 21.32/21.36: migrated inventory/consumption demos must use scalar scope identifiers.
declare -A retired_demo_scope_checks=(
  ["api/src/main/java/com/comandos/demo/AssetTraceabilityDemoVerifier.java"]='location\.organization\.|location\.unit\.'
  ["api/src/main/java/com/comandos/demo/ConsumableUsageLifecycleDemoVerifier.java"]='usage\.(organization|responsible|authorizer)([^A-Za-z0-9_]|$)'
  ["api/src/main/java/com/comandos/demo/DemoDataSeeder.java"]='StockLocation l where l\.organization[[:space:]]*='
  ["api/src/main/java/com/comandos/demo/EquipmentSetAggregateOperationDemoVerifier.java"]='location\.organization\.|set\.organization([^A-Za-z0-9_]|$)|operation\.organization([^A-Za-z0-9_]|$)'
  ["api/src/main/java/com/comandos/demo/MaintenanceDemoSeeder.java"]='asset\.location\.organization\.|asset\.location\.unit\.'
)

for retired_demo_file in "${!retired_demo_scope_checks[@]}"; do
  retired_demo_pattern="${retired_demo_scope_checks[$retired_demo_file]}"
  retired_demo_scope_hits="$(
    grep -nE "$retired_demo_pattern" "$ROOT_DIR/$retired_demo_file" 2>/dev/null || true
  )"
  if [[ -n "$retired_demo_scope_hits" ]]; then
    printf '%s:%s\n' "$retired_demo_file" "$retired_demo_scope_hits"
    fail "demo code still uses retired scalarized scope association: $retired_demo_file"
  fi
done

# Step 21.32/21.36: demo seeders must not use retired Master Data JPA associations.
for retired_demo_assignment in \
  'donation.organization =' \
  'donation.unit =' \
  'donation.donor =' \
  'donation.donee =' \
  'sale.organization =' \
  'sale.unit =' \
  'sale.buyer =' \
  'process.organization =' \
  'process.unit =' \
  'custody.organization =' \
  'custody.unit =' \
  'custody.recipient =' \
  'custody.authorizer =' \
  'consumption.organization =' \
  'consumption.unit =' \
  'consumption.responsible =' \
  'consumption.authorizer =' \
  'reservation.organization =' \
  'reservation.unit =' \
  'transfer.organization =' \
  'transfer.sourceUnit =' \
  'transfer.destinationUnit =' \
  'inventory.organization =' \
  'inventory.unit =' \
  'plan.organization =' \
  'plan.unit =' \
  'workOrder.organization =' \
  'workOrder.unit =' \
  'value.buyerOrganization =' \
  'value.supplierOrganization =' \
  'value.originPerson =' \
  'receiving.receivingOrganization ='
do
  retired_demo_hits="$(
    grep -RFn --include='*.java' "$retired_demo_assignment" \
      "$ROOT_DIR/api/src/main/java/com/comandos/demo" 2>/dev/null || true
  )"
  if [[ -n "$retired_demo_hits" ]]; then
    printf '%s\n' "$retired_demo_hits"
    fail "demo seeder still uses retired Master Data association: $retired_demo_assignment"
  fi
done

# Step 21.32/21.36: retired generic Java package roots must have no remaining imports.
for retired_java_package in \
  'com.comandos.model.' \
  'com.comandos.rest.' \
  'com.comandos.enterprise.' \
  'com.comandos.personnel.' \
  'com.comandos.procurement.'
do
  retired_package_hits="$(
    grep -RFn --include='*.java' "import $retired_java_package" "$ROOT_DIR/api/src" 2>/dev/null || true
  )"
  if [[ -n "$retired_package_hits" ]]; then
    printf '%s\n' "$retired_package_hits"
    fail "retired generic Java package import returned: $retired_java_package"
  fi
done

# Step 21.32/21.36: sales must not depend on the retired generic model package.
retired_payment_method_hits="$(
  grep -RFn --include='*.java' 'com.comandos.model.PaymentMethod' "$ROOT_DIR/api/src" 2>/dev/null || true
)"
if [[ -n "$retired_payment_method_hits" ]]; then
  printf '%s\n' "$retired_payment_method_hits"
  fail "sales still depends on retired com.comandos.model.PaymentMethod"
fi

SALES_SERVICE="$ROOT_DIR/api/src/main/java/com/comandos/sales/service/InventorySalesService.java"
if grep -q 'canonicalScope[[:space:]]*!=[[:space:]]*null' "$SALES_SERVICE"; then
  fail "InventorySalesService must not keep nullable ProductCanonicalScopeResolver fallbacks"
fi

# Step 21.36: retired generic API endpoints must not return.
for retired_endpoint in '/api/users' '/api/weapons' '/api/sales'; do
  retired_endpoint_hits="$(
    grep -RFn --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target \
      "$retired_endpoint" "$ROOT_DIR/api/src" "$ROOT_DIR/app/src" 2>/dev/null || true
  )"
  if [[ -n "$retired_endpoint_hits" ]]; then
    printf '%s\n' "$retired_endpoint_hits"
    fail "retired generic API endpoint returned: $retired_endpoint"
  fi
done

# Step 21.36: retired legacy/backup file names must not return.
while IFS= read -r legacy_path; do
  relative="${legacy_path#$ROOT_DIR/}"
  case "$relative" in
    "api/src/main/resources/db/oracle/compatibility-schema.sql" \
    |"api/src/test/java/com/comandos/inventory/model/StockLocationMasterDataBridgeTest.java" \
    |"api/src/main/java/com/comandos/core/service/ProductMasterDataReferenceBackfillService.java")
      ;;
    *)
      fail "unexpected legacy/backup compatibility file returned: $relative"
      ;;
  esac
done < <(
  find "$ROOT_DIR" -type f \
    \( -iname '*legacy*' -o -iname '*backup*' -o -iname '*.bak' -o -iname '*deprecated*' -o -iname '*copy*' -o -iname '*compatibility*' -o -iname '*bridge*' -o -iname '*backfill*' \) \
    -not -path '*/.git/*' \
    -not -path '*/node_modules/*' \
    -not -path '*/target/*' \
    | sort
)

# Steps 21.13-21.15: legacy master-data retirement backlog may only shrink.
RETIREMENT_MANIFEST="$ROOT_DIR/architecture/step-21-master-data-retirement.json"
test -f "$RETIREMENT_MANIFEST" || fail "missing Step 21 master-data retirement manifest"

legacy_id_count="$(grep -RhoE --include='*.java' 'public[[:space:]]+Long[[:space:]]+[A-Za-z0-9_]*LegacyId[[:space:]]*;' "$ROOT_DIR/api/src/main/java" | wc -l | tr -d ' ')"
[[ "$legacy_id_count" -le 59 ]] || fail "LegacyId field count grew above the corrected Step 21.13 baseline of 59"

allowed_legacy_id_files="$(
  node -e '
    const fs = require("fs");
    const manifest = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    const paths = [...new Set(manifest.legacyIdRetirement.fields.map(entry => entry.path))].sort();
    process.stdout.write(paths.join("\n"));
  ' "$RETIREMENT_MANIFEST"
)"

while IFS= read -r absolute; do
  relative="${absolute#$ROOT_DIR/}"
  if ! grep -Fxq "$relative" <<<"$allowed_legacy_id_files"; then
    fail "new LegacyId field introduced outside the Step 21.13 retirement inventory: $relative"
  fi
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

# Step 21.19: inventory must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/inventory"; then
  fail "inventory code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Donation and custody product overlays must not read generic Master Data entities directly.
for domain in donation custody; do
  if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|OrganizationalUnit|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/$domain"; then
    fail "$domain must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
  fi
done

# Custody responsibility roles must be resolved through the canonical party-role directory.
if grep -q 'com\.comandos\.core\.model\.PersonRoleAssignment' \
  "$ROOT_DIR/api/src/main/java/com/comandos/custody/service/CustodyIssueFacade.java"; then
  fail "CustodyIssueFacade must not depend directly on legacy PersonRoleAssignment"
fi

# Custody responsibility persistence must remain scalar/canonical for people.
CUSTODY_RESPONSIBILITY_MODEL="$ROOT_DIR/api/src/main/java/com/comandos/custody/model/CustodyResponsibility.java"
if grep -qE 'com\.comandos\.core\.model\.Person|[[:space:]]Person[[:space:]]+responsiblePerson' \
  "$CUSTODY_RESPONSIBILITY_MODEL"; then
  fail "CustodyResponsibility must not regain a direct legacy Person JPA association"
fi
grep -q 'responsiblePersonLegacyId' "$CUSTODY_RESPONSIBILITY_MODEL" \
  || fail "CustodyResponsibility must retain the controlled responsiblePersonLegacyId bridge until final Master Data cutover"
grep -q 'responsiblePersonCanonicalId' "$CUSTODY_RESPONSIBILITY_MODEL" \
  || fail "CustodyResponsibility must retain the canonical responsible person identifier"

# Step 21.20: purchase/procurement must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|Person|OrganizationalUnit)' "$ROOT_DIR/api/src/main/java/com/comandos/purchase"; then
  fail "purchase/procurement code must consume canonical Master Data instead of legacy Organization/Person/Unit entities"
fi
if grep -q 'ManyToOne' "$ROOT_DIR/api/src/main/java/com/comandos/purchase/model/ProcurementProcess.java" \
   && grep -q 'Organization' "$ROOT_DIR/api/src/main/java/com/comandos/purchase/model/ProcurementProcess.java"; then
  fail "ProcurementProcess must not regain a legacy Organization JPA association"
fi

# Lifecycle must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/lifecycle"; then
  fail "lifecycle code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Consumption must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/consumption"; then
  fail "consumption code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Reconciliation must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/reconciliation"; then
  fail "reconciliation code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Reservation must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/reservation"; then
  fail "reservation code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Disposal must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/disposal"; then
  fail "disposal code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Donation must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/donation"; then
  fail "donation code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Step 21.21: sales/alienation must not depend on legacy master-data entities directly.
if grep -RInE --include='*.java' 'import[[:space:]]+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person)|em\.find\((Organization|OrganizationalUnit|Person)\.class|locked\((Organization|Person)\.class' "$ROOT_DIR/api/src/main/java/com/comandos/sales"; then
  fail "sales/alienation code must consume canonical Master Data instead of legacy Organization/Unit/Person entities"
fi

# Step 21.32: retired generic/legacy package roots must not return.
for retired_generic_root in enterprise personnel procurement model rest; do
  [[ ! -d "$ROOT_DIR/api/src/main/java/com/comandos/$retired_generic_root" ]] \
    || fail "retired generic/legacy COMANDOS root returned: $retired_generic_root"
done

# Step 21.32/21.36: the obsolete generic user/person CRUD must not return.
retired_user_paths=(
  "api/src/main/java/com/comandos/rest/users"
  "api/src/main/java/com/comandos/model/User.java"
  "api/src/main/java/com/comandos/model/repository/UserRepository.java"
  "app/src/components/users"
  "app/src/api/services/user.service.ts"
  "app/src/api/models/users"
)
for retired_user_path in "${retired_user_paths[@]}"; do
  [[ ! -e "$ROOT_DIR/$retired_user_path" ]] \
    || fail "legacy generic user CRUD returned: $retired_user_path"
done
for legacy_user_page in \
  "$ROOT_DIR/app/src/app/queries/users/page.tsx" \
  "$ROOT_DIR/app/src/app/registrations/users/page.tsx"; do
  grep -q 'redirect("/erp/core?section=people&resource=people")' "$legacy_user_page" \
    || fail "legacy user route must redirect to canonical people workspace: $legacy_user_page"
done

# Step 21.32/21.36: the obsolete generic weapon CRUD must not return.
retired_weapon_paths=(
  "api/src/main/java/com/comandos/rest/weapons"
  "api/src/main/java/com/comandos/model/Weapon.java"
  "api/src/main/java/com/comandos/model/repository/WeaponRepository.java"
  "app/src/components/weapons"
  "app/src/api/services/weapon.service.ts"
  "app/src/api/models/weapons"
)
for retired_weapon_path in "${retired_weapon_paths[@]}"; do
  [[ ! -e "$ROOT_DIR/$retired_weapon_path" ]] \
    || fail "legacy generic weapon CRUD returned: $retired_weapon_path"
done
for legacy_weapon_page in \
  "$ROOT_DIR/app/src/app/queries/weapons/page.tsx" \
  "$ROOT_DIR/app/src/app/registrations/weapons/page.tsx"; do
  grep -q 'redirect("/erp/inventory?section=catalog&resource=item-models")' "$legacy_weapon_page" \
    || fail "legacy weapon route must redirect to canonical inventory catalog: $legacy_weapon_page"
done

# Step 21.32/21.36: the obsolete generic sales stack must not return.
retired_sales_paths=(
  "api/src/main/java/com/comandos/rest/sales"
  "app/src/components/sales"
  "app/src/api/services/sale.service.ts"
  "app/src/api/models/sales"
)
for retired_sales_path in "${retired_sales_paths[@]}"; do
  [[ ! -e "$ROOT_DIR/$retired_sales_path" ]] \
    || fail "legacy generic sales flow returned: $retired_sales_path"
done
grep -q 'redirect("/erp/sales")' "$ROOT_DIR/app/src/app/sales/new-sale/page.tsx" \
  || fail "legacy sales URL must redirect to canonical /erp/sales"

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


# Step 21.16: Oracle retirement audit must remain read-only.
ORACLE_RETIREMENT_AUDIT="$ROOT_DIR/scripts/oracle-step-21-retirement-audit.sql"
test -f "$ORACLE_RETIREMENT_AUDIT" || fail "missing Step 21.16 Oracle retirement audit"
if grep -Eiq '^[[:space:]]*(drop|truncate|delete|update|insert|merge|alter)[[:space:]]' "$ORACLE_RETIREMENT_AUDIT"; then
  fail "Step 21.16 Oracle retirement audit must remain read-only"
fi

# Steps 21.27-21.30: dependency/config/script cleanup.
if grep -q '<artifactId>spring-security-crypto</artifactId>' "$ROOT_DIR/api/pom.xml"; then
  fail "spring-security-crypto must not be declared directly; starter-security already provides it"
fi

for retired_frontend_package in '@faria-miguel/enterprise' '@faria-miguel/builder-core'; do
  if grep -q "\"$retired_frontend_package\"" "$ROOT_DIR/app/package.json"; then
    fail "retired unused frontend dependency returned: $retired_frontend_package"
  fi
done

node "$ROOT_DIR/scripts/audit-frontend-dependencies.mjs"

retired_unused_frontend_packages=(
  '@base-ui/react'
  'class-variance-authority'
  'clsx'
  'react-number-format'
  'swr'
  'tailwind-merge'
  'tw-animate-css'
  'formik'
  'yup'
)
for retired_package in "${retired_unused_frontend_packages[@]}"; do
  grep -q "\"${retired_package}@[^\"]*\":" "$ROOT_DIR/app/yarn.lock" \
    && fail "retired unused Yarn entry returned: $retired_package" || true
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