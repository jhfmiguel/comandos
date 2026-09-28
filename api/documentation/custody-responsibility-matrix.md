# Custody responsibility matrix

This matrix is normative for custody issuance.

| Scope | Recipient | Responsible person | Eligible assignment | Duration |
| --- | --- | --- | --- | --- |
| INDIVIDUAL | PERSON | recipient | not required by institutional policy | TEMPORARY or PERMANENT |
| COLLECTIVE | UNIT | mandatory | active, currently valid assignment in the receiving unit with an authorized responsibility role | TEMPORARY or PERMANENT |
| TEAM | UNIT | mandatory | active, currently valid assignment in the receiving unit with an authorized responsibility role | TEMPORARY only |
| OPERATION | UNIT | mandatory | active, currently valid assignment in the receiving unit with an authorized responsibility role | TEMPORARY only |

## Duration rules

- `TEMPORARY` requires `dueAt`.
- `PERMANENT` forbids `dueAt`.
- `TEAM` and `OPERATION` are always temporary.
- `TEAM` and `OPERATION` require `teamOperation` identification.
- `COLLECTIVE` represents custody assigned to an organizational unit and does not use `teamOperation`.

## Institutional responsibility

Institutional custody is issued through `POST /api/erp/custodies/institutional`.

The responsible person must have a `PersonRoleAssignment` that:

1. belongs to the same person;
2. belongs to the same organization;
3. belongs specifically to the receiving unit;
4. has status `ACTIVE`;
5. has already started and has not expired;
6. references an authorized responsibility role.

Authorized role codes are currently:

- `CUSTODY_RESPONSIBLE` / `RESPONSAVEL_CAUTELA`
- `MATERIAL_RESPONSIBLE` / `RESPONSAVEL_MATERIAL`
- `ARMORY_MANAGER` / `RESPONSAVEL_ARMAMENTO`
- `WAREHOUSE_MANAGER` / `ALMOXARIFE`
- `UNIT_HEAD` / `CHEFE_UNIDADE`
- `UNIT_MANAGER` / `GESTOR_UNIDADE`
- `TEAM_LEADER` / `CHEFE_EQUIPE`
- `OPERATION_COMMANDER` / `COMANDANTE_OPERACAO`

The exact assignment used at issuance is persisted in `erp_custody_responsibility`, together with snapshots of the responsible person's name, role, organization and unit. This provides historical accountability even if the person's assignment changes later.

## Compatibility rule

`POST /api/erp/custodies` is restricted to `INDIVIDUAL` + `PERSON`. Institutional, collective, team and operation custody must use the institutional endpoint so responsibility validation cannot be bypassed.
