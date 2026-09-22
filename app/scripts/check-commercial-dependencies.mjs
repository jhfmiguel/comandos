import { access, readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const packageJsonText = await readFile(resolve(root, "package.json"), "utf8");
const yarnLock = await readFile(resolve(root, "yarn.lock"), "utf8");
const packageJson = JSON.parse(packageJsonText);

const primeTokens = [
  "@primereact/",
  "primereact@",
  "@primeui/",
  "@primeuix/",
  "@primeicons/",
  "primeicons@"
];

const temporaryPrimePackages = new Set([
  "@primereact/ui",
  "primereact",
  "@primeicons/react",
  "primeicons",
  "@primeuix/themes"
]);

let migrationMode = false;
try {
  await access(resolve(root, "..", "docs", "prime-removal-migration.md"));
  migrationMode = true;
} catch {}

const directPrime = Object.keys({
  ...(packageJson.dependencies ?? {}),
  ...(packageJson.devDependencies ?? {})
}).filter(name =>
  name === "primereact" ||
  name === "primeicons" ||
  name.startsWith("@primereact/") ||
  name.startsWith("@primeui/") ||
  name.startsWith("@primeuix/") ||
  name.startsWith("@primeicons/")
);

const unexpectedDirectPrime = directPrime.filter(name => !temporaryPrimePackages.has(name));

if (unexpectedDirectPrime.length) {
  console.error("Unexpected Prime dependency introduced:");
  for (const name of unexpectedDirectPrime) console.error(`- ${name}`);
  process.exit(1);
}

const hasPrimeInLock = primeTokens.some(token =>
  yarnLock.toLowerCase().includes(token.toLowerCase())
);

if (directPrime.length || hasPrimeInLock) {
  console.error("Prime dependencies remain after migration:");
  for (const name of directPrime) console.error(`- direct: ${name}`);
  if (hasPrimeInLock) console.error("- Prime package remains in yarn.lock");
  process.exit(1);
}

console.log("Prime dependencies: none.");

console.log("Commercial dependency policy: PASS");
