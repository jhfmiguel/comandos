import { readFile } from "node:fs/promises";
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
  "primeicons@"
];


const directPrime = Object.keys({
  ...(packageJson.dependencies ?? {}),
  ...(packageJson.devDependencies ?? {})
}).filter(name =>
  name === "primereact" ||
  name === "primeicons" ||
  name.startsWith("@primereact/") ||
  name.startsWith("@primeui/") ||
  name.startsWith("@primeuix/") ||
  (name.startsWith("@primeicons/") && name !== "@primeicons/react")
);

const hasPrimeInLock = primeTokens.some(token =>
  yarnLock.toLowerCase().includes(token.toLowerCase())
);

if (directPrime.length || hasPrimeInLock) {
  console.error("Prime dependencies remain after migration:");
  for (const name of directPrime) console.error(`- direct: ${name}`);
  if (hasPrimeInLock) console.error("- Prime package remains in yarn.lock");
  process.exit(1);
}

console.log("Prime component/theme dependencies: none.");
console.log("Approved icon dependency: @primeicons/react.");

console.log("Commercial dependency policy: PASS");
