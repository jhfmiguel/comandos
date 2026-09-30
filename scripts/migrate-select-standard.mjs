import fs from "node:fs";

function ensureImport(source) {
  const importLine = 'import { ComandosSelectField } from "components/common/select-field";';
  if (source.includes(importLine)) return source;
  const marker = 'import * as React from "react";';
  if (source.includes(marker)) return source.replace(marker, `${marker}\n${importLine}`);
  throw new Error("React import marker not found");
}

function parseOptions(block) {
  return [...block.matchAll(/<option(?:\s+value="([^"]*)")?[^>]*>([^<]+)<\/option>/g)]
    .map(([, value, label]) => ({ value: value ?? label.trim(), label: label.trim() }));
}

function optionSource(options) {
  return options.map(option => `{ value: ${JSON.stringify(option.value)}, label: ${JSON.stringify(option.label)} }`).join(",\n                                    ");
}

const queryPath = "app/src/components/erp/armament-query/index.tsx";
let query = ensureImport(fs.readFileSync(queryPath, "utf8"));
const queryPattern = /<select id="query-status" name="status" defaultValue=\{params\.get\("status"\) \|\| ""\}>[\s\S]*?<\/select>/;
if (!queryPattern.test(query)) throw new Error("armament-query status select not found");
query = query.replace(queryPattern, `<ComandosSelectField
                                id="query-status"
                                name="status"
                                label="Situação"
                                value={params.get("status") || ""}
                                placeholder="Todos"
                                options={statuses.map(status => ({ value: status, label: status }))}
                                onChange={() => {}}
                            />`);
fs.writeFileSync(queryPath, query);

const purchasePath = "app/src/components/erp/purchases/index.tsx";
let purchase = fs.readFileSync(purchasePath, "utf8");
const purchasePattern = /<select aria-label="Next procurement status" disabled=\{statusBusy\}[\s\S]*?<\/select>/;
const purchaseMatch = purchase.match(purchasePattern);
if (!purchaseMatch) throw new Error("purchase procurement select not found");
const options = parseOptions(purchaseMatch[0]).filter(option => option.value !== "");
if (!options.length) throw new Error("purchase procurement options not found");
purchase = purchase.replace(purchasePattern, `<ComandosSelectField
                        id="next-procurement-status"
                        label="Next procurement status"
                        disabled={statusBusy}
                        value=""
                        placeholder="Advance procurement…"
                        options={[
                                    ${optionSource(options)}
                        ]}
                        onChange={value => {
                            if (value) void advanceStatus(value as ProcurementStatus);
                        }}
                    />`);
fs.writeFileSync(purchasePath, purchase);

console.log("Canonical select migration applied to armament query and purchases.");
