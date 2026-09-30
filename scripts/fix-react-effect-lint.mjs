import fs from "node:fs";

function update(path, transform) {
  const before = fs.readFileSync(path, "utf8");
  const after = transform(before);
  if (after === before) console.log(`${path}: already compliant`);
  else {
    fs.writeFileSync(path, after);
    console.log(`${path}: updated`);
  }
}

update("app/src/app/dashboard/page.tsx", source =>
  source.replace(
    /React\.useEffect\(\(\) => \{\s*void load\(\);\s*\}, \[load\]\);/m,
    `React.useEffect(() => {\n        queueMicrotask(() => void load());\n    }, [load]);`
  )
);

for (const path of [
  "app/src/components/erp/shared/nested-core-editors.tsx",
  "app/src/components/erp/shared/nested-resource-editors.tsx",
  "app/src/components/erp/shared/record-workspace.tsx"
]) {
  update(path, source => source.replaceAll(
    `const controller = new AbortController();\n        setLoading(true);`,
    `const controller = new AbortController();\n        queueMicrotask(() => {\n            if (!controller.signal.aborted) setLoading(true);\n        });`
  ));
}

update("app/src/components/erp/shared/person-contacts-editor.tsx", source =>
  source.replaceAll(
    `const controller = new AbortController();\n        setCountriesLoading(true);`,
    `const controller = new AbortController();\n        queueMicrotask(() => {\n            if (!controller.signal.aborted) setCountriesLoading(true);\n        });`
  )
);

update("app/src/components/erp/shared/nested-resource-editors.tsx", source => {
  let next = source;
  if (!next.includes("const stableFilters = React.useMemo<Record<string, string>>")) {
    next = next.replace(
      `const filterKey = JSON.stringify(filters);\n    const [options`,
      `const filterKey = JSON.stringify(filters);\n    const stableFilters = React.useMemo<Record<string, string>>(() => JSON.parse(filterKey), [filterKey]);\n    const [options`
    );
  }
  next = next.replace(
    `            filters,\n            100\n        ).then(result => {`,
    `            stableFilters,\n            100\n        ).then(result => {`
  );
  next = next.replace(
    `    }, [service, resource, organizationId, filterKey]);`,
    `    }, [service, resource, organizationId, stableFilters]);`
  );
  return next;
});

update("app/src/components/erp/purchases/index.tsx", source =>
  source.replace(
    `function money(value: number | string | undefined, _locale: "pt-BR" | "en-US"): string {`,
    `function money(value: number | string | undefined): string {`
  ).replaceAll(/money\(([^,\n]+),\s*locale\)/g, "money($1)")
);

console.log("React effect lint migration completed.");
