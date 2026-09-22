import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const packageJson = await readFile(resolve(root, "package.json"), "utf8");
const yarnLock = await readFile(resolve(root, "yarn.lock"), "utf8");

const forbidden = [
  "@primereact/",
  "primereact@",
  "@primeui/",
  "@primeuix/",
  "@primeicons/",
  "primeicons@"
];

const haystacks = [
  ["package.json", packageJson],
  ["yarn.lock", yarnLock]
];

const violations = [];
for (const [file, content] of haystacks) {
  for (const token of forbidden) {
    if (content.toLowerCase().includes(token.toLowerCase())) {
      violations.push(`${file}: ${token}`);
    }
  }
}

if (violations.length) {
  console.error("Commercial dependency policy violation:");
  for (const violation of violations) console.error(`- ${violation}`);
  process.exit(1);
}

console.log("Commercial dependency policy: PASS");
