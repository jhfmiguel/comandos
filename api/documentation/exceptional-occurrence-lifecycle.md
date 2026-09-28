# Exceptional occurrence lifecycle

## Scope

COMANDOS exceptional occurrences now use an explicit state-transition matrix for loss, theft, robbery, seizure, recovery, damage, accident, recall and investigation while preserving organization/unit, actor, investigation and document history.

Primary API: `/api/erp/lifecycle/occurrences`.

## Canonical matrix

| Business event | Canonical type | Opening effect | Resolution effect |
| --- | --- | --- | --- |
| Extravio | `LOSS` | serialized asset becomes `MISSING`; lot becomes `BLOCKED` | stays unavailable until a linked recovery |
| Furto | `THEFT` | serialized asset becomes `MISSING`; lot becomes `BLOCKED` | stays unavailable until a linked recovery |
| Roubo | `ROBBERY` | serialized asset becomes `MISSING`; lot becomes `BLOCKED` | stays unavailable until a linked recovery |
| Apreensão | `SEIZURE` | asset/lot becomes `BLOCKED` | remains blocked after administrative resolution |
| Recuperação | `RECOVERY` | missing asset becomes `BLOCKED` while recovery is investigated | returns to `AVAILABLE` only after documented resolution |
| Dano | `DAMAGE` | asset/lot becomes `BLOCKED` | remains blocked; maintenance/inspection owns subsequent release |
| Acidente | `ACCIDENT` | asset/lot becomes `BLOCKED` | remains blocked; maintenance/inspection owns subsequent release |
| Recolhimento | `RECALL` | asset/lot becomes `BLOCKED` | remains blocked until the appropriate lifecycle process releases it |
| Investigação | `INVESTIGATION` | no availability transition by itself | no availability transition by itself |

Legacy aliases such as `LOST`, `RECOVERED`, `BLOCK` and `DIVERGENCE` remain readable where already persisted.

## Investigation workflow

Occurrence workflow states are:

1. `OPEN` — event registered;
2. `UNDER_INVESTIGATION` — investigation notes, investigator and start timestamp recorded;
3. `RESOLVED` — investigation conclusion, resolver and resolution timestamp recorded.

Endpoint:

- `POST /api/erp/lifecycle/occurrences/{id}/investigate`
- `POST /api/erp/lifecycle/occurrences/{id}/resolve`

Resolution requires investigation notes. Theft, robbery, seizure and recovery also require a document reference.

## Recovery integrity

Recovery is deliberately stricter than the previous generic occurrence handling:

- recovery is allowed only for an individually identified asset;
- the asset must currently be `MISSING`;
- the request must reference the original `LOSS`, `THEFT` or `ROBBERY` occurrence;
- both occurrences must reference the same asset;
- opening recovery changes the asset from `MISSING` to `BLOCKED`, not directly to `AVAILABLE`;
- only resolution of the documented recovery returns the asset to `AVAILABLE`;
- resolving recovery also closes the related missing occurrence.

This preserves the missing period in history and prevents a recovered asset from silently becoming available before the recovery is investigated.

## Terminal-state protection

Assets/lots in terminal ownership/disposition states (`SOLD`, `DONATED`, `DISPOSED`) cannot receive new exceptional occurrences that reactivate them. Occurrence resolution is also forbidden from using recovery to bypass definitive sale, donation or disposal.

Damage, accident, seizure and recall resolutions do not automatically set assets back to `AVAILABLE`; release remains the responsibility of the relevant inspection/maintenance/administrative flow.

## Historical snapshots

`ExceptionOccurrence` now preserves:

- related occurrence for recovery chains;
- previous item status;
- resulting item status;
- investigation start timestamp;
- investigator id/login;
- occurrence actor;
- resolver id/login and resolution timestamp;
- investigation notes and document reference.

These fields make it possible to reconstruct the state transition without relying only on the asset or lot's current status.

## Regression

`ExceptionalOccurrenceDemoVerifier` runs in the demo/Oracle business-regression gate and validates:

- the nine-event matrix is complete;
- occurrence scope and responsibility history;
- valid workflow states;
- exactly one asset or lot per occurrence;
- missing-state preservation for loss/theft/robbery;
- blocked-state preservation for seizure/damage/accident/recall;
- recovery provenance and same-asset relationship;
- recovery starts from `MISSING` and remains `BLOCKED` until resolution;
- investigation alone does not modify availability;
- legal occurrence document requirements;
- terminal assets are not reactivated through recovery.
