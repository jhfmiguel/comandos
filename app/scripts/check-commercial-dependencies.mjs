import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const packageJsonText = await readFile(resolve(root, "package.json"), "utf8");
const yarnLock = await readFile(resolve(root, "yarn.lock"), "utf8");
const packageJson = JSON.parse(packageJsonText);

const approvedPrimePackages = new Set([
  "@primeicons/react",
  "@primeicons/core",
  "@primeuix/utils"
]);

const isPrimeFamily = (name) =>
  name === "primereact" ||
  name === "primeicons" ||
  name.startsWith("@primereact/") ||
  name.startsWith("@primeui/") ||
  name.startsWith("@primeuix/") ||
  name.startsWith("@primeicons/");

const directPrime = Object.keys({
  ...(packageJson.dependencies ?? {}),
  ...(packageJson.devDependencies ?? {})
}).filter(name => isPrimeFamily(name) && !approvedPrimePackages.has(name));

const lockPackageNames = yarnLock
  .split("\n")
  .filter(line => line && !/^\s/.test(line) && line.endsWith(":"))
  .flatMap(line => {
    const normalized = line.replace(/^"|"?:$/g, "");
    const scoped = normalized.match(/^(@[^/]+\/[^@"]+)@/);
    if (scoped) return [scoped[1]];

    const unscoped = normalized.match(/^([^@"\s]+)@/);
    return unscoped ? [unscoped[1]] : [];
  });

const forbiddenPrimeInLock = [...new Set(lockPackageNames)]
  .filter(name => isPrimeFamily(name) && !approvedPrimePackages.has(name));

if (directPrime.length || forbiddenPrimeInLock.length) {
  console.error("Unapproved Prime dependencies remain after migration:");
  for (const name of directPrime) console.error(`- direct: ${name}`);
  for (const name of forbiddenPrimeInLock) console.error(`- lockfile: ${name}`);
  process.exit(1);
}

console.log("Prime component/theme dependencies: none.");
console.log("Approved icon stack: @primeicons/react, @primeicons/core, @primeuix/utils.");
console.log("Commercial dependency policy: PASS");
