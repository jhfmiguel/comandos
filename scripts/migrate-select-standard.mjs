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
const queryNativePattern = /<select id="query-status" name="status" defaultValue=\{params\.get\("status"\) \|\| ""\}>[\s\S]*?<\/select>/;
const querySharedPattern = /<ComandosSelectField\s+id="query-status"[\s\S]*?\/>/;
const queryReplacement = `<ComandosSelectField
                                id="query-status"
                                name="status"
                                label="Situação"
                                value={params.get("status") || ""}
                                placeholder="Todos"
                                options={statuses.map(status => ({ value: status, label: status }))}
                                onChange={value => {
                                    const next = new URLSearchParams(query);
                                    if (value) next.set("status", value);
                                    else next.delete("status");
                                    next.delete("page");
                                    next.delete("asset");
                                    navigate(next);
                                }}
                            />`;
if (queryNativePattern.test(query)) query = query.replace(queryNativePattern, queryReplacement);
else if (querySharedPattern.test(query)) query = query.replace(querySharedPattern, queryReplacement);
else throw new Error("armament-query status selector not found");
fs.writeFileSync(queryPath, query);

const purchasePath = "app/src/components/erp/purchases/index.tsx";
let purchase = fs.readFileSync(purchasePath, "utf8");
const purchaseNativePattern = /<select aria-label="Next procurement status" disabled=\{statusBusy\}[\s\S]*?<\/select>/;
const purchaseSharedPattern = /<ComandosSelectField\s+id="next-procurement-status"[\s\S]*?\/>/;
const purchaseMatch = purchase.match(purchaseNativePattern);
if (purchaseMatch) {
  const options = parseOptions(purchaseMatch[0]).filter(option => option.value !== "");
  if (!options.length) throw new Error("purchase procurement options not found");
  purchase = purchase.replace(purchaseNativePattern, `<ComandosSelectField
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
} else if (!purchaseSharedPattern.test(purchase)) {
  throw new Error("purchase procurement selector not found");
}
fs.writeFileSync(purchasePath, purchase);

console.log("Canonical select migration applied and behavior preserved.");
