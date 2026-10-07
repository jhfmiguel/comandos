import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const lock = JSON.parse(fs.readFileSync(path.join(root, 'architecture', 'faria-miguel-gates.lock.json'), 'utf8'));
const retirement = JSON.parse(fs.readFileSync(path.join(root, 'architecture', 'step-21-master-data-retirement.json'), 'utf8'));
const foreignKeyInventory = JSON.parse(fs.readFileSync(path.join(root, 'architecture', 'master-data-foreign-keys.json'), 'utf8'));

const IGNORED = new Set(['.git','.next','.turbo','coverage','dist','build','target','node_modules','migration-staging']);
const violations = [];
const legacyIdInventory = retirement.legacyIdRetirement;
const expectedLegacyIds = new Set((legacyIdInventory.fields ?? []).map(({path: filePath, field}) => `${filePath}#${field}`));
const detectedLegacyIds = new Set();
const scalarizedMasterDataModels = new Set((legacyIdInventory.fields ?? []).map(({path: filePath}) => filePath));
const foreignKeyReferencesByPath = new Map((foreignKeyInventory.references ?? []).map(reference => [reference.path, reference]));
const oracleRetirementAuditPath = path.join(root, 'scripts', 'oracle-step-21-retirement-audit.sql');
const oracleRetirementAudit = fs.readFileSync(oracleRetirementAuditPath, 'utf8');
const oracleBridgePairs = new Set(
  [...oracleRetirementAudit.matchAll(/select '([^']+)'[,\s]*'([^']+)' from dual/g)]
    .map(match => match[1] + '#' + match[2])
);
const requiredCutoverFiles = new Set([
  ...(retirement.legacyMasterDataEntities?.files ?? []),
  ...(retirement.legacyMasterDataCode?.compatibilityFiles ?? []),
  ...(retirement.cutoverRuntimeSupport?.files ?? [])
]);

function walk(dir, visit) {
  if (!fs.existsSync(dir)) return;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory() && IGNORED.has(entry.name)) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, visit);
    else visit(full);
  }
}
function rel(file){ return path.relative(root,file).split(path.sep).join('/'); }
function ownJava(pkg){ return lock.product.javaNamespaces.some(p => pkg === p || pkg.startsWith(p + '.')); }
function ownFrontend(name){ return lock.product.frontendPackagePrefixes.some(p => name === p || name.startsWith(p + '/') || name.startsWith(p + '-')); }
function starts(value,prefixes){ return prefixes.some(p => value === p || value.startsWith(p + '.') || value.startsWith(p + '/')); }
function productArtifactId(xml){
  const x = xml.replace(/<parent>[\s\S]*?<\/parent>/i,'');
  const head = x.split(/<dependencies>|<dependencyManagement>|<build>|<profiles>|<modules>|<repositories>|<distributionManagement>/i)[0];
  return head.match(/<artifactId>\s*([^<]+?)\s*<\/artifactId>/i)?.[1]?.trim() ?? null;
}
function depBlocks(xml){ return [...xml.matchAll(/<dependency>([\s\S]*?)<\/dependency>/gi)].map(m=>m[1]); }
function exactFrontendVersion(v){
  if (typeof v !== 'string') return null;
  if (v.startsWith('npm:')) {
    const at=v.lastIndexOf('@');
    return at>3 ? v.slice(at+1) : null;
  }
  return /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/.test(v) ? v : null;
}

const reservedJava = [
  'com.fariamiguel.core','com.fariamiguel.identity','com.fariamiguel.security',
  'com.fariamiguel.session','com.fariamiguel.preferences','com.fariamiguel.audit',
  'com.fariamiguel.workflow','com.fariamiguel.documents','com.fariamiguel.messaging',
  'com.fariamiguel.notifications','com.fariamiguel.analytics','com.fariamiguel.privacy',
  'com.fariamiguel.enterprise'
];
const reservedArtifacts = new Set([
  'faria-miguel-platform','faria-miguel-platform-migrations',
  'faria-miguel-enterprise','faria-miguel-enterprise-persistence-jpa'
]);
const siblingProducts = lock.products.filter(p => p.id !== lock.product.id);

walk(root, file => {
  const relative = rel(file);
  const base = path.basename(file);

  if (file.endsWith('.java')) {
    const src=fs.readFileSync(file,'utf8');
    if (relative.startsWith('api/src/main/java/') && relative.includes('/model/')) {
      const legacyField = /\b(?:public|protected|private)[ \t]+(?:(?:static|final|transient|volatile)[ \t]+)*(?:[\w.$<>?,\[\]]+)[ \t]+([A-Za-z_][A-Za-z0-9_]*LegacyId)[ \t]*(?:=[^;\n]*)?;/g;
      for (const match of src.matchAll(legacyField)) detectedLegacyIds.add(relative + '#' + match[1]);
    }
    if (scalarizedMasterDataModels.has(relative)) {
      const legacyMasterDataAssociationImport = /^\s*import\s+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person|PersonRoleAssignment)\s*;/m;
      const legacyMasterDataAssociationField = /@(ManyToOne|OneToOne)[\s\S]{0,240}?\b(Organization|OrganizationalUnit|Person|PersonRoleAssignment)\s+[A-Za-z_][A-Za-z0-9_]*\s*;/m;
      if (legacyMasterDataAssociationImport.test(src) || legacyMasterDataAssociationField.test(src)) {
        violations.push(relative + ' reintroduces a legacy Master Data JPA association after scalarization');
      }
    }
    if (relative.includes('/model/')
        && !relative.includes('/core/model/')
        && relative !== 'api/src/main/java/com/comandos/compliance/model/CompliancePolicy.java') {
      const legacyMasterDataModelImport = /^\s*import\s+com\.comandos\.core\.model\.(Organization|OrganizationalUnit|Person|PersonRoleAssignment)\s*;/m;
      if (legacyMasterDataModelImport.test(src)) {
        violations.push(relative + ' introduces a product-model dependency on legacy Master Data');
      }
    }
    const pkg=src.match(/^\s*package\s+([\w.]+)\s*;/m)?.[1];
    if (pkg) {
      for (const reserved of reservedJava) {
        if (pkg === reserved || pkg.startsWith(reserved + '.')) {
          violations.push(relative + ' redeclares shared core namespace ' + pkg);
          break;
        }
      }
      if ((pkg === 'com.fariamiguel' || pkg.startsWith('com.fariamiguel.')) && !ownJava(pkg)) {
        violations.push(relative + ' declares shared namespace ' + pkg + ' outside product namespace');
      }
    }
    for (const m of src.matchAll(/^\s*import\s+([\w.]+)(?:\.\*)?\s*;/gm)) {
      const ref=m[1];
      for (const sibling of siblingProducts) {
        if (starts(ref,sibling.javaNamespaces)) violations.push(relative + ' imports sibling product ' + sibling.id + ': ' + ref);
      }
    }
    return;
  }

  if (base === 'pom.xml') {
    const xml=fs.readFileSync(file,'utf8');
    const ownArtifact=productArtifactId(xml);
    if (reservedArtifacts.has(ownArtifact)) violations.push(relative + ' redeclares reserved Maven artifactId ' + ownArtifact);

    const prop=xml.match(/<faria-miguel\.version>\s*([^<]+?)\s*<\/faria-miguel\.version>/i)?.[1]?.trim();
    if (prop && prop !== lock.mavenVersion) violations.push(relative + ' has faria-miguel.version=' + prop + ', expected ' + lock.mavenVersion);

    for (const block of depBlocks(xml)) {
      const group=block.match(/<groupId>\s*([^<]+?)\s*<\/groupId>/i)?.[1]?.trim();
      const artifact=block.match(/<artifactId>\s*([^<]+?)\s*<\/artifactId>/i)?.[1]?.trim();
      const version=block.match(/<version>\s*([^<]+?)\s*<\/version>/i)?.[1]?.trim();
      if (group === 'com.fariamiguel' && artifact?.startsWith('faria-miguel-')) {
        if (!version) violations.push(relative + ' shared dependency ' + artifact + ' has no version');
        else if (version !== '${faria-miguel.version}' && version !== lock.mavenVersion) violations.push(relative + ' shared dependency ' + artifact + ' uses ' + version + ', expected ' + lock.mavenVersion);
        else if (version === '${faria-miguel.version}' && prop !== lock.mavenVersion) violations.push(relative + ' uses canonical property without matching ' + lock.mavenVersion);
      }
      for (const sibling of siblingProducts) {
        if (group && starts(group,sibling.mavenGroupPrefixes)) violations.push(relative + ' depends on sibling product ' + sibling.id + ' via Maven group ' + group);
      }
    }
    return;
  }

  if (base === 'package.json') {
    let json;
    try { json=JSON.parse(fs.readFileSync(file,'utf8')); }
    catch { violations.push('invalid package.json: ' + relative); return; }

    if (typeof json.name === 'string') {
      if (json.name === '@faria-miguel/platform' || json.name === '@faria-miguel/enterprise') violations.push(relative + ' redeclares reserved frontend package ' + json.name);
      if (json.name.startsWith('@faria-miguel/') && !ownFrontend(json.name)) violations.push(relative + ' declares shared frontend namespace ' + json.name);
    }

    for (const section of ['dependencies','devDependencies','peerDependencies','optionalDependencies']) {
      for (const [name,value] of Object.entries(json[section] ?? {})) {
        if (name.startsWith('@faria-miguel/')) {
          const actual=exactFrontendVersion(value);
          if (actual !== lock.frontendVersion) violations.push(relative + ' dependency ' + name + ' uses ' + value + ', expected exact ' + lock.frontendVersion);
        }
        for (const sibling of siblingProducts) {
          if (starts(name,sibling.frontendPackagePrefixes)) violations.push(relative + ' depends on sibling product ' + sibling.id + ' via package ' + name);
          if (String(value).includes(sibling.repository)) violations.push(relative + ' references sibling repository ' + sibling.repository);
        }
      }
    }
    return;
  }

  if (/\.(?:ts|tsx|js|jsx|mjs|cjs)$/.test(file)) {
    const src=fs.readFileSync(file,'utf8');
    const refs=[];
    for (const re of [/\bfrom\s+['"]([^'"]+)['"]/g,/\brequire\(\s*['"]([^'"]+)['"]\s*\)/g,/\bimport\(\s*['"]([^'"]+)['"]\s*\)/g]) {
      for (const m of src.matchAll(re)) refs.push(m[1]);
    }
    for (const ref of refs) for (const sibling of siblingProducts) {
      if (starts(ref,sibling.frontendPackagePrefixes)) violations.push(relative + ' imports sibling product ' + sibling.id + ': ' + ref);
    }
  }
});

const baselineCount = Number(legacyIdInventory.baselineCount);
const currentCount = Number(legacyIdInventory.currentCount);
if (!Number.isInteger(baselineCount) || baselineCount < 0) {
  violations.push('Step 21 LegacyId baselineCount must be a non-negative integer');
}
if (!Number.isInteger(currentCount) || currentCount < 0) {
  violations.push('Step 21 LegacyId currentCount must be a non-negative integer');
}
if (currentCount > baselineCount) {
  violations.push('Step 21 LegacyId currentCount=' + currentCount + ' exceeds baselineCount=' + baselineCount);
}
if (expectedLegacyIds.size !== currentCount) {
  violations.push('Step 21 LegacyId manifest inventory has ' + expectedLegacyIds.size + ' entries but currentCount=' + currentCount);
}
for (const key of detectedLegacyIds) {
  if (!expectedLegacyIds.has(key)) violations.push('untracked *LegacyId field introduced: ' + key);
}
for (const key of expectedLegacyIds) {
  if (!detectedLegacyIds.has(key)) violations.push('Step 21 LegacyId manifest is stale or field moved/removed without inventory update: ' + key);
}
if (detectedLegacyIds.size > baselineCount) {
  violations.push('detected *LegacyId field count=' + detectedLegacyIds.size + ' exceeds immutable Step 21 baseline=' + baselineCount);
}
if (detectedLegacyIds.size !== currentCount) {
  violations.push('detected *LegacyId field count=' + detectedLegacyIds.size + ' differs from locked currentCount=' + currentCount);
}
if (oracleBridgePairs.size !== currentCount) {
  violations.push('Oracle Step 21 retirement audit tracks ' + oracleBridgePairs.size + ' unique legacy bridge columns, expected currentCount=' + currentCount);
}
for (const modelPath of scalarizedMasterDataModels) {
  const reference = foreignKeyReferencesByPath.get(modelPath);
  if (!reference) {
    violations.push('scalarized Master Data model missing from master-data-foreign-keys.json: ' + modelPath);
  } else if (reference.state !== 'retired') {
    violations.push('scalarized Master Data model must remain retired from legacy JPA association: ' + modelPath + ' state=' + reference.state);
  }
}
for (const reference of foreignKeyInventory.references ?? []) {
  if (reference.state !== 'retired') {
    violations.push('Step 21 Master Data FK retirement regressed: ' + reference.entity + ' state=' + reference.state);
  }
}
for (const requiredFile of requiredCutoverFiles) {
  if (!fs.existsSync(path.join(root, requiredFile))) {
    violations.push('required Step 21 cutover/compatibility file is missing before retirement gate PASS: ' + requiredFile);
  }
}

if (violations.length) {
  console.error('Faria Miguel automatic gate violations:');
  for (const v of [...new Set(violations)]) console.error(' - ' + v);
  process.exit(1);
}
console.log('Faria Miguel automatic gates passed.');
console.log('Foundation SHA: ' + lock.foundationSha);
console.log('Maven version: ' + lock.mavenVersion + ' | Frontend version: ' + lock.frontendVersion);
console.log('Step 21 *LegacyId inventory: ' + detectedLegacyIds.size + '/' + baselineCount + ' (current/baseline).');
console.log('Step 21 scalarized Master Data models guarded: ' + scalarizedMasterDataModels.size + '.');
console.log('Step 21 Oracle legacy bridge columns guarded: ' + oracleBridgePairs.size + '.');
console.log('Step 21 FK retirement manifest covers scalarized models: ' + scalarizedMasterDataModels.size + '.');
console.log('Step 21 FK retirement entries locked retired: ' + (foreignKeyInventory.references ?? []).length + '.');
console.log('Step 21 required cutover/compatibility files guarded: ' + requiredCutoverFiles.size + '.');
console.log('Step 21 product models globally guarded from legacy Master Data imports (CompliancePolicy explicitly excepted).');
