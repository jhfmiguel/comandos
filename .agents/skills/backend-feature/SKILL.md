# Backend Feature Skill

Use this skill for backend feature work in COMANDOS.

## Scope
Java/Spring Boot APIs, domain services, persistence, migrations, security, audit, events, tests.

## Procedure
1. Read AGENTS.md and identify the owning domain.
2. Inspect existing contracts, tests, migrations, and neighboring workflows.
3. Reuse Platform Core only through public/reusable contracts.
4. Keep domain rules inside the owning module.
5. Prefer canonical enums/value objects/contracts over raw duplicated strings.
6. Preserve auditability and authorization checks.
7. Add or update automated tests for every critical rule and regression fixed.
8. Run relevant Maven tests before completion.
9. Document contract or architectural changes.
10. Commit one principal responsibility at a time.

## Guardrails
- Do not broaden Platform Core without a demonstrated cross-domain requirement.
- Do not bypass workflow state transitions.
- Do not weaken authorization/audit to make a test pass.
- Do not commit secrets or production data.
