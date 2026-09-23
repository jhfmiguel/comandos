import { readdir, readFile } from "node:fs/promises"
import { extname, join, relative, resolve } from "node:path"

const root = resolve(import.meta.dirname, "..")
const platformRoot = resolve(root, "src", "platform")

const forbidden = [
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

for (const file of await filesUnder(platformRoot)) {
    const source = await readFile(file, "utf8")

    for (const token of forbidden) {
        if (source.includes(token)) {
            violations.push(
                `${relative(root, file)} imports forbidden domain token: ${token}`
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
