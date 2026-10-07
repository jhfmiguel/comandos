import { readdir, readFile } from "node:fs/promises"
import { extname, join, relative, resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")
const srcRoot = resolve(root, "src")
const platformRoot = resolve(srcRoot, "platform")

const forbiddenPlatformTokens = [
    "components/erp/",
    "components/weapons/",
    "components/sales/",
    "api/services/ammunition-",
    "api/services/armament-",
    "api/services/custody",
    "api/services/disposal",
    "api/services/donation",
    "api/services/inventory-",
    "api/services/lifecycle",
    "api/services/maintenance",
    "api/services/purchase",
    "api/services/receiving-",
    "api/services/reservation",
    "api/services/sale",
    "api/services/transfer",
    "api/services/weapon"
]

const retiredFrontendImports = [
    "platform/components/data-table",
    "platform/components/form-field",
    "platform/components/pagination",
    "platform/components/confirm-dialog",
    "platform/data",
    "platform/hooks",
    "platform/shared-foundation",
    "platform/error-boundary"
]

async function filesUnder(directory) {
    const entries = await readdir(directory, { withFileTypes: true })
    const files = []

    for (const entry of entries) {
        const path = join(directory, entry.name)

        if (entry.isDirectory()) {
            files.push(...await filesUnder(path))
            continue
        }

        if ([".ts", ".tsx"].includes(extname(entry.name))) {
            files.push(path)
        }
    }

    return files
}

const violations = []
const sourceFiles = await filesUnder(srcRoot)

for (const file of sourceFiles) {
    const source = await readFile(file, "utf8")
    const relativePath = relative(root, file)

    for (const token of retiredFrontendImports) {
        if (source.includes(token)) {
            violations.push(
                `${relativePath} references retired frontend import: ${token}`
            )
        }
    }

    if (!file.startsWith(platformRoot)) continue

    for (const token of forbiddenPlatformTokens) {
        if (source.includes(token)) {
            violations.push(
                `${relativePath} imports forbidden domain token: ${token}`
            )
        }
    }
}

if (violations.length) {
    console.error("Platform frontend boundary violations:")
    for (const violation of violations) console.error(`- ${violation}`)
    process.exit(1)
}

console.log("Platform frontend boundary: PASS")
