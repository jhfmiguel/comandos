import { readdir, readFile } from "node:fs/promises";
import { extname, join, relative, resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const srcRoot = resolve(root, "src");
const sourceExtensions = new Set([".tsx", ".jsx"]);

async function filesUnder(directory) {
    const entries = await readdir(directory, { withFileTypes: true });
    const files = [];

    for (const entry of entries) {
        const path = join(directory, entry.name);

        if (entry.isDirectory()) {
            files.push(...await filesUnder(path));
            continue;
        }

        if (sourceExtensions.has(extname(entry.name))) {
            files.push(path);
        }
    }

    return files;
}

const violations = [];

for (const file of await filesUnder(srcRoot)) {
    const source = await readFile(file, "utf8");

    if (/<select\b/i.test(source)) {
        violations.push(
            `${relative(root, file)} uses a native <select>; use ComandosSelectField instead.`
        );
    }
}

if (violations.length) {
    console.error("Select component standard violations:");
    for (const violation of violations) console.error(`- ${violation}`);
    process.exit(1);
}

console.log("Select component standard: PASS");
