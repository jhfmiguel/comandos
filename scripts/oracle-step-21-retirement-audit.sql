set pagesize 500
set linesize 240
set trimspool on
set feedback on
set verify off

prompt ============================================================
prompt COMANDOS - Step 21.16 Oracle legacy retirement audit
prompt Read-only inventory. This script performs no DDL or DML.
prompt ============================================================

prompt
prompt [1] Known retired generic CRUD tables, if they still exist
select table_name
from user_tables
where upper(table_name) in ('USER', 'WEAPON')
order by table_name;

prompt
prompt [2] Row counts for known retired generic CRUD tables
declare
    l_count number;
begin
    for r in (
        select table_name
        from user_tables
        where upper(table_name) in ('USER', 'WEAPON')
        order by table_name
    ) loop
        execute immediate 'select count(*) from "' || replace(r.table_name, '"', '""') || '"' into l_count;
        dbms_output.put_line(r.table_name || ': ' || l_count || ' row(s)');
    end loop;
end;
/

prompt
prompt [3] Master Data crosswalk / migration tables
select table_name
from user_tables
where upper(table_name) like '%MASTER%DATA%REFERENCE%'
   or upper(table_name) like '%CROSSWALK%'
order by table_name;

prompt
prompt [4] Expected legacy scalar bridge columns still present (59-field Java baseline)

with expected_bridge_columns (table_name, column_name) as (
    select 'ERP_STOCK_LOCATION','ORGANIZATION_ID' from dual union all
    select 'ERP_STOCK_LOCATION','UNIT_ID' from dual union all
    select 'ERP_PURCHASE','BUYER_ORGANIZATION_ID' from dual union all
    select 'ERP_PURCHASE','SUPPLIER_ORGANIZATION_ID' from dual union all
    select 'ERP_PURCHASE','ORIGIN_PERSON_ID' from dual union all
    select 'ERP_EQUIPMENT_RECEIVING','RECEIVING_ORGANIZATION_ID' from dual union all
    select 'ERP_PROCUREMENT_PROCESS','ORGANIZATION_ID' from dual union all
    select 'ERP_CUSTODY','ORGANIZATION_ID' from dual union all
    select 'ERP_CUSTODY','UNIT_ID' from dual union all
    select 'ERP_CUSTODY','RECIPIENT_ID' from dual union all
    select 'ERP_CUSTODY','RECIPIENT_UNIT_ID' from dual union all
    select 'ERP_CUSTODY','AUTHORIZER_ID' from dual union all
    select 'ERP_DONATION','ORGANIZATION_ID' from dual union all
    select 'ERP_DONATION','UNIT_ID' from dual union all
    select 'ERP_DONATION','DONOR_ID' from dual union all
    select 'ERP_DONATION','DONEE_ID' from dual union all
    select 'ERP_SALE','ORGANIZATION_ID' from dual union all
    select 'ERP_SALE','UNIT_ID' from dual union all
    select 'ERP_SALE','BUYER_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','SOURCE_UNIT_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','DESTINATION_UNIT_ID' from dual union all
    select 'ERP_WORK_ORDER','ORGANIZATION_ID' from dual union all
    select 'ERP_WORK_ORDER','UNIT_ID' from dual union all
    select 'ERP_APPROVAL_WORKFLOW','ORGANIZATION_ID' from dual union all
    select 'ERP_APPROVAL_WORKFLOW','UNIT_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','ORGANIZATION_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','UNIT_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','RESPONSIBLE_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','AUTHORIZER_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','ORGANIZATION_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','UNIT_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','RESPONSIBLE_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','AUTHORIZER_ID' from dual union all
    select 'ERP_DISPOSAL_PROCESS','ORGANIZATION_ID' from dual union all
    select 'ERP_DISPOSAL_PROCESS','UNIT_ID' from dual union all
    select 'ERP_INVENTORY_RESERVATION','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_RESERVATION','UNIT_ID' from dual union all
    select 'ERP_INVENTORY_COUNT','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_COUNT','UNIT_ID' from dual union all
    select 'ERP_PURCHASE_PLANNING','ORGANIZATION_ID' from dual union all
    select 'ERP_PERIODIC_INSPECTION','ORGANIZATION_ID' from dual union all
    select 'ERP_PERIODIC_INSPECTION','UNIT_ID' from dual union all
    select 'ERP_EXCEPTION_OCCURRENCE','ORGANIZATION_ID' from dual union all
    select 'ERP_EXCEPTION_OCCURRENCE','UNIT_ID' from dual union all
    select 'ERP_EQUIPMENT_SET','ORGANIZATION_ID' from dual union all
    select 'ERP_EQUIPMENT_SET','UNIT_ID' from dual union all
    select 'ERP_EQUIPMENT_SET_OPERATION','ORGANIZATION_ID' from dual union all
    select 'ERP_EQUIPMENT_SET_OPERATION','UNIT_ID' from dual union all
    select 'ERP_CERTIFICATION_RECORD','ORGANIZATION_ID' from dual union all
    select 'ERP_CERTIFICATION_RECORD','UNIT_ID' from dual union all
    select 'ERP_EXPIRATION_RECORD','ORGANIZATION_ID' from dual union all
    select 'ERP_EXPIRATION_RECORD','UNIT_ID' from dual union all
    select 'ERP_RECALL','ORGANIZATION_ID' from dual union all
    select 'ERP_RECALL','UNIT_ID' from dual union all
    select 'ERP_CUSTODY_RESPONSIBILITY','RESPONSIBLE_PERSON_ID' from dual union all
    select 'ERP_CUSTODY_RESPONSIBILITY','ROLE_ASSIGNMENT_ID' from dual union all
    select 'ERP_MAINTENANCE_PLAN','ORGANIZATION_ID' from dual union all
    select 'ERP_MAINTENANCE_PLAN','UNIT_ID' from dual
)
select e.table_name,
       e.column_name,
       c.data_type,
       c.nullable,
       case when c.column_name is null then 'MISSING' else 'PRESENT' end as inventory_status
from expected_bridge_columns e
left join user_tab_columns c
  on c.table_name = e.table_name
 and c.column_name = e.column_name
order by e.table_name, e.column_name;

prompt
prompt [5] Canonical shadow/reference columns used by cutover
select table_name, column_name, data_type, nullable
from user_tab_columns
where upper(column_name) like '%CANONICAL%ID%'
order by table_name, column_id;

prompt
prompt [6] Indexes tied to legacy/canonical cutover columns or crosswalks

with expected_bridge_columns (table_name, column_name) as (
    select 'ERP_STOCK_LOCATION','ORGANIZATION_ID' from dual union all
    select 'ERP_STOCK_LOCATION','UNIT_ID' from dual union all
    select 'ERP_PURCHASE','BUYER_ORGANIZATION_ID' from dual union all
    select 'ERP_PURCHASE','SUPPLIER_ORGANIZATION_ID' from dual union all
    select 'ERP_PURCHASE','ORIGIN_PERSON_ID' from dual union all
    select 'ERP_EQUIPMENT_RECEIVING','RECEIVING_ORGANIZATION_ID' from dual union all
    select 'ERP_PROCUREMENT_PROCESS','ORGANIZATION_ID' from dual union all
    select 'ERP_CUSTODY','ORGANIZATION_ID' from dual union all
    select 'ERP_CUSTODY','UNIT_ID' from dual union all
    select 'ERP_CUSTODY','RECIPIENT_ID' from dual union all
    select 'ERP_CUSTODY','RECIPIENT_UNIT_ID' from dual union all
    select 'ERP_CUSTODY','AUTHORIZER_ID' from dual union all
    select 'ERP_DONATION','ORGANIZATION_ID' from dual union all
    select 'ERP_DONATION','UNIT_ID' from dual union all
    select 'ERP_DONATION','DONOR_ID' from dual union all
    select 'ERP_DONATION','DONEE_ID' from dual union all
    select 'ERP_SALE','ORGANIZATION_ID' from dual union all
    select 'ERP_SALE','UNIT_ID' from dual union all
    select 'ERP_SALE','BUYER_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','SOURCE_UNIT_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','DESTINATION_UNIT_ID' from dual union all
    select 'ERP_WORK_ORDER','ORGANIZATION_ID' from dual union all
    select 'ERP_WORK_ORDER','UNIT_ID' from dual union all
    select 'ERP_APPROVAL_WORKFLOW','ORGANIZATION_ID' from dual union all
    select 'ERP_APPROVAL_WORKFLOW','UNIT_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','ORGANIZATION_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','UNIT_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','RESPONSIBLE_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','AUTHORIZER_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','ORGANIZATION_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','UNIT_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','RESPONSIBLE_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','AUTHORIZER_ID' from dual union all
    select 'ERP_DISPOSAL_PROCESS','ORGANIZATION_ID' from dual union all
    select 'ERP_DISPOSAL_PROCESS','UNIT_ID' from dual union all
    select 'ERP_INVENTORY_RESERVATION','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_RESERVATION','UNIT_ID' from dual union all
    select 'ERP_INVENTORY_COUNT','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_COUNT','UNIT_ID' from dual union all
    select 'ERP_PURCHASE_PLANNING','ORGANIZATION_ID' from dual union all
    select 'ERP_PERIODIC_INSPECTION','ORGANIZATION_ID' from dual union all
    select 'ERP_PERIODIC_INSPECTION','UNIT_ID' from dual union all
    select 'ERP_EXCEPTION_OCCURRENCE','ORGANIZATION_ID' from dual union all
    select 'ERP_EXCEPTION_OCCURRENCE','UNIT_ID' from dual union all
    select 'ERP_EQUIPMENT_SET','ORGANIZATION_ID' from dual union all
    select 'ERP_EQUIPMENT_SET','UNIT_ID' from dual union all
    select 'ERP_EQUIPMENT_SET_OPERATION','ORGANIZATION_ID' from dual union all
    select 'ERP_EQUIPMENT_SET_OPERATION','UNIT_ID' from dual union all
    select 'ERP_CERTIFICATION_RECORD','ORGANIZATION_ID' from dual union all
    select 'ERP_CERTIFICATION_RECORD','UNIT_ID' from dual union all
    select 'ERP_EXPIRATION_RECORD','ORGANIZATION_ID' from dual union all
    select 'ERP_EXPIRATION_RECORD','UNIT_ID' from dual union all
    select 'ERP_RECALL','ORGANIZATION_ID' from dual union all
    select 'ERP_RECALL','UNIT_ID' from dual union all
    select 'ERP_CUSTODY_RESPONSIBILITY','RESPONSIBLE_PERSON_ID' from dual union all
    select 'ERP_CUSTODY_RESPONSIBILITY','ROLE_ASSIGNMENT_ID' from dual union all
    select 'ERP_MAINTENANCE_PLAN','ORGANIZATION_ID' from dual union all
    select 'ERP_MAINTENANCE_PLAN','UNIT_ID' from dual
)
select distinct i.index_name, i.table_name, c.column_name, i.status
from user_indexes i
join user_ind_columns c
  on c.index_name = i.index_name
left join expected_bridge_columns e
  on e.table_name = i.table_name
 and e.column_name = c.column_name
where e.column_name is not null
   or upper(c.column_name) like '%CANONICAL%ID%'
   or upper(i.table_name) like '%MASTER%DATA%REFERENCE%'
order by i.table_name, i.index_name, c.column_name;

prompt
prompt [7] Constraints / foreign keys tied to legacy/canonical columns

with expected_bridge_columns (table_name, column_name) as (
    select 'ERP_STOCK_LOCATION','ORGANIZATION_ID' from dual union all
    select 'ERP_STOCK_LOCATION','UNIT_ID' from dual union all
    select 'ERP_PURCHASE','BUYER_ORGANIZATION_ID' from dual union all
    select 'ERP_PURCHASE','SUPPLIER_ORGANIZATION_ID' from dual union all
    select 'ERP_PURCHASE','ORIGIN_PERSON_ID' from dual union all
    select 'ERP_EQUIPMENT_RECEIVING','RECEIVING_ORGANIZATION_ID' from dual union all
    select 'ERP_PROCUREMENT_PROCESS','ORGANIZATION_ID' from dual union all
    select 'ERP_CUSTODY','ORGANIZATION_ID' from dual union all
    select 'ERP_CUSTODY','UNIT_ID' from dual union all
    select 'ERP_CUSTODY','RECIPIENT_ID' from dual union all
    select 'ERP_CUSTODY','RECIPIENT_UNIT_ID' from dual union all
    select 'ERP_CUSTODY','AUTHORIZER_ID' from dual union all
    select 'ERP_DONATION','ORGANIZATION_ID' from dual union all
    select 'ERP_DONATION','UNIT_ID' from dual union all
    select 'ERP_DONATION','DONOR_ID' from dual union all
    select 'ERP_DONATION','DONEE_ID' from dual union all
    select 'ERP_SALE','ORGANIZATION_ID' from dual union all
    select 'ERP_SALE','UNIT_ID' from dual union all
    select 'ERP_SALE','BUYER_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','SOURCE_UNIT_ID' from dual union all
    select 'ERP_INVENTORY_TRANSFER','DESTINATION_UNIT_ID' from dual union all
    select 'ERP_WORK_ORDER','ORGANIZATION_ID' from dual union all
    select 'ERP_WORK_ORDER','UNIT_ID' from dual union all
    select 'ERP_APPROVAL_WORKFLOW','ORGANIZATION_ID' from dual union all
    select 'ERP_APPROVAL_WORKFLOW','UNIT_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','ORGANIZATION_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','UNIT_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','RESPONSIBLE_ID' from dual union all
    select 'ERP_AMMUNITION_CONSUMPTION','AUTHORIZER_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','ORGANIZATION_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','UNIT_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','RESPONSIBLE_ID' from dual union all
    select 'ERP_CONSUMABLE_USAGE','AUTHORIZER_ID' from dual union all
    select 'ERP_DISPOSAL_PROCESS','ORGANIZATION_ID' from dual union all
    select 'ERP_DISPOSAL_PROCESS','UNIT_ID' from dual union all
    select 'ERP_INVENTORY_RESERVATION','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_RESERVATION','UNIT_ID' from dual union all
    select 'ERP_INVENTORY_COUNT','ORGANIZATION_ID' from dual union all
    select 'ERP_INVENTORY_COUNT','UNIT_ID' from dual union all
    select 'ERP_PURCHASE_PLANNING','ORGANIZATION_ID' from dual union all
    select 'ERP_PERIODIC_INSPECTION','ORGANIZATION_ID' from dual union all
    select 'ERP_PERIODIC_INSPECTION','UNIT_ID' from dual union all
    select 'ERP_EXCEPTION_OCCURRENCE','ORGANIZATION_ID' from dual union all
    select 'ERP_EXCEPTION_OCCURRENCE','UNIT_ID' from dual union all
    select 'ERP_EQUIPMENT_SET','ORGANIZATION_ID' from dual union all
    select 'ERP_EQUIPMENT_SET','UNIT_ID' from dual union all
    select 'ERP_EQUIPMENT_SET_OPERATION','ORGANIZATION_ID' from dual union all
    select 'ERP_EQUIPMENT_SET_OPERATION','UNIT_ID' from dual union all
    select 'ERP_CERTIFICATION_RECORD','ORGANIZATION_ID' from dual union all
    select 'ERP_CERTIFICATION_RECORD','UNIT_ID' from dual union all
    select 'ERP_EXPIRATION_RECORD','ORGANIZATION_ID' from dual union all
    select 'ERP_EXPIRATION_RECORD','UNIT_ID' from dual union all
    select 'ERP_RECALL','ORGANIZATION_ID' from dual union all
    select 'ERP_RECALL','UNIT_ID' from dual union all
    select 'ERP_CUSTODY_RESPONSIBILITY','RESPONSIBLE_PERSON_ID' from dual union all
    select 'ERP_CUSTODY_RESPONSIBILITY','ROLE_ASSIGNMENT_ID' from dual union all
    select 'ERP_MAINTENANCE_PLAN','ORGANIZATION_ID' from dual union all
    select 'ERP_MAINTENANCE_PLAN','UNIT_ID' from dual
)
select uc.constraint_name,
       uc.constraint_type,
       uc.table_name,
       ucc.column_name,
       uc.status,
       uc.r_constraint_name
from user_constraints uc
join user_cons_columns ucc
  on ucc.constraint_name = uc.constraint_name
left join expected_bridge_columns e
  on e.table_name = uc.table_name
 and e.column_name = ucc.column_name
where e.column_name is not null
   or upper(ucc.column_name) like '%CANONICAL%ID%'
   or upper(uc.table_name) like '%MASTER%DATA%REFERENCE%'
order by uc.table_name, uc.constraint_name, ucc.position;

prompt
prompt [8] Views containing obvious legacy/cutover markers
select view_name
from user_views
where upper(text_vc) like '%LEGACY%ID%'
   or upper(text_vc) like '%CANONICAL%ID%'
   or upper(text_vc) like '%MASTER_DATA_REFERENCE%'
order by view_name;

prompt
prompt [9] Triggers on known legacy/cutover tables
select trigger_name, table_name, status, triggering_event
from user_triggers
where upper(table_name) in ('USER', 'WEAPON', 'ERP_MASTER_DATA_REFERENCE')
   or upper(trigger_body) like '%LEGACY%ID%'
   or upper(trigger_body) like '%CANONICAL%ID%'
   or upper(trigger_body) like '%MASTER_DATA_REFERENCE%'
order by table_name, trigger_name;

prompt
prompt [10] Sequences with legacy/master-data naming signals
select sequence_name, last_number
from user_sequences
where upper(sequence_name) like '%LEGACY%'
   or upper(sequence_name) like '%MASTER%DATA%'
   or upper(sequence_name) like '%USER%'
   or upper(sequence_name) like '%WEAPON%'
order by sequence_name;

prompt
prompt [11] Retirement rule reminder
prompt - USER/WEAPON tables: verify row preservation/migration before drop.
prompt - The 59 mapped scalar bridge fields use physical names such as ORGANIZATION_ID/UNIT_ID/etc.; do not drop them before Step 21.12 cutover PASS.
prompt - ERP_MASTER_DATA_REFERENCE is the durable cutover crosswalk and must also remain until Step 21.12 PASS.
prompt - *_CANONICAL_ID columns are not legacy by themselves; retain if they are the target model.
prompt - Review every result before generating destructive DDL.
