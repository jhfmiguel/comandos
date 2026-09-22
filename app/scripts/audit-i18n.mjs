import fs from "node:fs"
import path from "node:path"

const root = path.resolve(process.cwd(), "src")
const catalogPath = path.join(
    root,
    "components",
    "settings",
    "i18n-catalog.ts"
)

const catalog = fs.readFileSync(catalogPath, "utf8")

const known = new Set()

for (const match of catalog.matchAll(/\["((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)"\]/g)) {
    known.add(JSON.parse(`"${match[1]}"`))
    known.add(JSON.parse(`"${match[2]}"`))
}

const files = []

function walk(directory) {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
        const full = path.join(directory, entry.name)

        if (entry.isDirectory()) {
            walk(full)
            continue
        }

        if (
            /\.(tsx|ts)$/.test(entry.name) &&
            !entry.name.includes(".backup-") &&
            !entry.name.endsWith(".d.ts") &&
            full !== catalogPath
        ) {
            files.push(full)
        }
    }
}

walk(root)

const candidates = []

const ignored = [
    /^use client$/,
    /^GET$/,
    /^POST$/,
    /^PUT$/,
    /^PATCH$/,
    /^DELETE$/,
    /^READ$/,
    /^CREATE$/,
    /^UPDATE$/,
    /^MANAGE$/,
    /^SYSTEM$/,
    /^ORGANIZATION$/,
    /^UNIT$/,
    /^PERSON$/,
    /^https?:\/\//,
    /^\//,
    /^[a-z0-9./_-]+$/i
]

function shouldIgnore(value) {
    const text = value.trim()

    if (text.length < 2) return true
    if (!/[A-Za-zÀ-ÿ]/.test(text)) return true
    if (ignored.some((rule) => rule.test(text))) return true
    if (known.has(text)) return true

    return false
}

for (const file of files) {
    const source = fs.readFileSync(file, "utf8")
    const lines = source.split(/\r?\n/)

    lines.forEach((line, index) => {
        const matches = [
            ...line.matchAll(/>([^<>{}][^<>{]*[A-Za-zÀ-ÿ][^<>{}]*)</g),
            ...line.matchAll(/\b(?:title|placeholder|aria-label|label|description)=["']([^"']*[A-Za-zÀ-ÿ][^"']*)["']/g),
            ...line.matchAll(/\b(?:text|caption):\s*["']([^"']*[A-Za-zÀ-ÿ][^"']*)["']/g)
        ]

        for (const match of matches) {
            const value = match[1].trim()

            if (!shouldIgnore(value)) {
                candidates.push({
                    file: path.relative(process.cwd(), file),
                    line: index + 1,
                    text: value
                })
            }
        }
    })
}

const unique = new Map()

for (const item of candidates) {
    const key = `${item.file}:${item.line}:${item.text}`
    unique.set(key, item)
}

const results = [...unique.values()].sort((a, b) =>
    a.file.localeCompare(b.file) ||
    a.line - b.line ||
    a.text.localeCompare(b.text)
)

const reportPath = path.resolve(
    process.cwd(),
    "..",
    "automation",
    "i18n-audit.txt"
)

const report = [
    "COMANDOS - I18N AUDIT",
    `Generated: ${new Date().toISOString()}`,
    `Potential untranslated literals: ${results.length}`,
    "",
    ...results.map(
        (item) => `${item.file}:${item.line} | ${item.text}`
    )
].join("\n")

fs.writeFileSync(reportPath, report, "utf8")

console.log(report)
console.log("")
console.log(`Report: ${reportPath}`)

if (results.length > 0) {
    process.exitCode = 2
}
