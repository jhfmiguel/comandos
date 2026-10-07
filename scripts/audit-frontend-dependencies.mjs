import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const appRoot = path.join(root, "app");
const packageFile = path.join(appRoot, "package.json");

const ignoredDirectories = new Set([
  "node_modules",
  ".next",
  ".yarn",
  "dist",
  "build",
  "coverage"
]);

const ignoredFiles = new Set([
  "package.json",
  "yarn.lock",
  "package-lock.json",
  "pnpm-lock.yaml"
]);

const textExtensions = new Set([
  ".ts",
  ".tsx",
  ".js",
  ".jsx",
  ".mjs",
  ".cjs",
  ".css",
  ".json"
]);

function walk(directory) {
  if (!fs.existsSync(directory)) return [];

  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    if (ignoredDirectories.has(entry.name)) return [];

    const full = path.join(directory, entry.name);

    if (entry.isDirectory()) return walk(full);
    if (ignoredFiles.has(entry.name)) return [];
    if (!textExtensions.has(path.extname(entry.name))) return [];

    return [full];
  });
}

function referencedFiles(dependency, files) {
  return files
    .filter((file) => fs.readFileSync(file, "utf8").includes(dependency))
    .map((file) => path.relative(root, file).replaceAll("\\", "/"));
}

const manifest = JSON.parse(fs.readFileSync(packageFile, "utf8"));
const files = walk(appRoot);

const runtimeDependencies = Object.keys(manifest.dependencies ?? {}).sort();
const devDependencies = Object.keys(manifest.devDependencies ?? {}).sort();

const implicitRuntime = new Set([
  "react",
  "react-dom"
]);

const implicitDev = new Set([
  "@types/node",
  "@types/react",
  "@types/react-dom",
  "eslint",
  "eslint-config-next",
  "typescript",
  "shadcn"
]);

function audit(dependencies, implicit) {
  return dependencies.map((dependency) => {
    const references = referencedFiles(dependency, files);
    return {
      dependency,
      references,
      implicit: implicit.has(dependency),
      used: references.length > 0 || implicit.has(dependency)
    };
  });
}

const runtimeAudit = audit(runtimeDependencies, implicitRuntime);
const devAudit = audit(devDependencies, implicitDev);

const unusedRuntime = runtimeAudit.filter((entry) => !entry.used);
const unusedDev = devAudit.filter((entry) => !entry.used);

console.log("Step 21.28 frontend dependency audit");
console.log("Runtime dependencies:");
for (const entry of runtimeAudit) {
  console.log(
    `- ${entry.dependency}: ${entry.used ? "USED" : "UNUSED"}`
      + (entry.implicit ? " (framework runtime)" : "")
      + (entry.references.length ? ` -> ${entry.references.join(", ")}` : "")
  );
}

console.log("Development dependencies:");
for (const entry of devAudit) {
  console.log(
    `- ${entry.dependency}: ${entry.used ? "USED" : "UNUSED"}`
      + (entry.implicit ? " (toolchain)" : "")
      + (entry.references.length ? ` -> ${entry.references.join(", ")}` : "")
  );
}

if (unusedDev.length) {
  console.log(
    "Development dependencies requiring manual/tooling review: "
      + unusedDev.map((entry) => entry.dependency).join(", ")
  );
}

if (unusedRuntime.length) {
  console.error(
    "Unused runtime dependencies: "
      + unusedRuntime.map((entry) => entry.dependency).join(", ")
  );
  process.exit(2);
}

console.log("Step 21.28 runtime dependency audit PASS.");
