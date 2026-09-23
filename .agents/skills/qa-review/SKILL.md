# QA Review Skill

Use this skill for independent review. Default behavior is review-first, not scope expansion.

## Review areas
- correctness and regression risk;
- authorization and scope enforcement;
- audit trail completeness;
- state-transition integrity;
- concurrency/idempotency;
- validation and error handling;
- API/frontend contract consistency;
- persistence/migration safety;
- accessibility and loading/error states;
- dependency/license policy;
- missing tests.

## Procedure
1. Read AGENTS.md and the changed commits/diff.
2. Reproduce or reason through critical paths.
3. Identify concrete defects with file/rule evidence.
4. Prefer adding regression tests for confirmed defects.
5. Do not redesign unrelated modules during a QA task.
6. Report unresolved risks explicitly.
