import { readFile } from "node:fs/promises";

const files = process.argv.slice(2);
if (!files.length) {
  console.error("Usage: node scripts/check-sbom-licenses.mjs <bom.json> [...]");
  process.exit(2);
}

const forbiddenPatterns = [
  /AGPL/i,
  /SSPL/i,
  /BUSL/i,
  /Business Source/i,
  /Commons Clause/i,
  /PolyForm/i,
  /Elastic License/i,
  /PrimeUI/i
];

const reviewPatterns = [
  /GPL/i,
  /LGPL/i,
  /MPL/i,
  /EPL/i,
  /CDDL/i,
  /Oracle/i,
  /FUTC/i,
  /UNKNOWN/i,
  /NOASSERTION/i,
  /UNLICENSED/i
];

let failed = false;
const review = [];

function licenseText(component) {
  const licenses = component.licenses ?? [];
  if (!licenses.length) return "UNKNOWN";
  return licenses.map(entry =>
    entry?.license?.id ??
    entry?.license?.name ??
    entry?.expression ??
    "UNKNOWN"
  ).join(" OR ");
}

for (const file of files) {
  const bom = JSON.parse(await readFile(file, "utf8"));
  const components = bom.components ?? [];
  console.log(`\nSBOM: ${file} (${components.length} components)`);

  for (const component of components) {
    const license = licenseText(component);
    const name = `${component.name ?? "unknown"}@${component.version ?? "unknown"}`;

    if (forbiddenPatterns.some(pattern => pattern.test(license))) {
      failed = true;
      console.error(`BLOCKED: ${name} -> ${license}`);
      continue;
    }

    if (reviewPatterns.some(pattern => pattern.test(license))) {
      review.push({ file, name, license });
    }
  }
}

if (review.length) {
  console.log("\nManual/legal review list:");
  for (const item of review) {
    console.log(`- ${item.name}: ${item.license} [${item.file}]`);
  }
}

if (failed) {
  console.error("\nSBOM license policy failed.");
  process.exit(1);
}

console.log("\nSBOM license policy passed. Review-listed licenses are not automatically rejected.");
