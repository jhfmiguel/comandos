# AGENTS.md

## Product
COMANDOS is a modular ERP/platform for public and private security management.


## Product vision
COMANDOS is not an armament system. It is a broad, long-term management platform for public and private security, designed to cover the widest practical set of operational, administrative, logistical, intelligence, compliance, asset, personnel, planning, reporting, integration, and command-and-control capabilities found in the market.

Armament is only the first major vertical domain. Architecture and implementation decisions must preserve room for current and future domains such as transport/fleet, escort, intelligence, operations, incidents, crisis management, personnel, shifts, facilities, custody/movements, procurement, contracts, suppliers, training, documents/processes, risk/compliance, analytics, integrations, and other security-management capabilities.

Do not optimize the architecture only for the current Armament module.
Do not add speculative complexity without a concrete use case, but avoid domain choices that would prevent future expansion.

## Source of truth
- The `main` branch is the official integration branch.
- GitHub is the canonical project history.
- Keep commits small, cohesive, and independently reviewable.
- Prefer one principal responsibility per commit.

## Architecture
- Backend: Java 25, Spring Boot, PostgreSQL, Spring Modulith.
- Frontend: Next.js, React, TypeScript.
- Reusable platform code belongs to Platform Core only when it is domain-neutral.
- Business domains depend on Platform Core, never the reverse.
- Do not expand Platform Core preventively. Add to it only when a real cross-domain need exists.
- Armament and future vertical modules must preserve explicit boundaries.

## Engineering rules
- Never silently remove existing behavior during refactoring.
- Critical business rules require automated tests.
- Prefer canonical enums/contracts over duplicated strings.
- Inventory and equipment history must remain auditable and traceable.
- Do not bypass domain workflows by editing operational state directly.
- Preserve backward compatibility unless the task explicitly authorizes a breaking change.
- Do not introduce a commercial/proprietary dependency without explicit approval.
- Do not commit credentials, private keys, tokens, production secrets, personal data, or customer data.
- Run the relevant tests, lint, build, license checks, and architecture checks before declaring work complete.

## Agent roles
- Coordinator/Architect: plans boundaries, sequencing, contracts, and cross-project reuse.
- Backend Agent: API, domain rules, persistence, migrations, security, audit, tests.
- Frontend Agent: Next.js/React UI, accessibility, API integration, client tests.
- QA Agent: reviews behavior, regression risk, security, contracts, edge cases; does not add product scope by default.
- Release Agent: CI/CD, dependency/license review, SBOM, build/release readiness.

See `docs/engineering/agent-responsibilities.md`.

## Parallel work
- Use a separate branch/worktree for each concurrent task.
- Never let two agents edit the same files concurrently unless explicitly coordinated.
- One agent owns a file set during a task.
- Integrate through reviewed commits/PRs or an explicitly coordinated merge.
- Rebase/update from `main` before final validation.

See `docs/engineering/git-worktrees.md`.

## Skills
Reusable procedures live under `.agents/skills/`.
Use the smallest relevant skill and follow repository-specific rules before generic guidance.

## Intellectual property and dependencies
- This repository is private proprietary software unless explicitly stated otherwise.
- Do not copy source code to unrelated repositories, public snippets, examples, or external services.
- Reuse across COMANDOS, Tubarão, and Trator must be deliberate and owned by the same personal development environment.
- Do not mix code, secrets, documentation, or automation from institutional PPGO repositories into this repository.
- Follow `docs/legal/intellectual-property.md` and `docs/legal/dependency-policy.md`.

## Completion standard
A task is complete only when:
1. requested behavior is implemented;
2. relevant tests pass;
3. no known regression is left hidden;
4. architecture boundaries remain valid;
5. documentation is updated when behavior/contracts changed;
6. commit history clearly explains the change.
