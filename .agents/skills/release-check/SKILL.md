# Release Check Skill

Use this skill before a release, deployment, or milestone approval.

## Checklist
- repository is based on current main;
- backend tests pass;
- frontend lint/build pass;
- architecture/platform boundary checks pass;
- license/dependency policy passes;
- SBOM generation succeeds when configured;
- database migrations are ordered and reversible/forward-safe as designed;
- no secrets or debug credentials are committed;
- production configuration is externalized;
- changelog/release notes identify breaking changes;
- critical workflows have smoke coverage;
- backup/rollback expectations are documented.

## Output
Produce a concise release readiness result with blockers separated from non-blocking observations.
Do not mark a release ready while a known critical blocker remains.
