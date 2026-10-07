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
prompt [4] Legacy scalar-id columns still present
select table_name, column_name, data_type, nullable
from user_tab_columns
where upper(column_name) like '%LEGACY%ID%'
order by table_name, column_id;

prompt
prompt [5] Canonical shadow/reference columns used by cutover
select table_name, column_name, data_type, nullable
from user_tab_columns
where upper(column_name) like '%CANONICAL%ID%'
order by table_name, column_id;

prompt
prompt [6] Indexes tied to legacy/canonical cutover columns or crosswalks
select distinct i.index_name, i.table_name, c.column_name, i.status
from user_indexes i
join user_ind_columns c
  on c.index_name = i.index_name
where upper(c.column_name) like '%LEGACY%ID%'
   or upper(c.column_name) like '%CANONICAL%ID%'
   or upper(i.table_name) like '%MASTER%DATA%REFERENCE%'
order by i.table_name, i.index_name, c.column_name;

prompt
prompt [7] Constraints / foreign keys tied to legacy/canonical columns
select uc.constraint_name,
       uc.constraint_type,
       uc.table_name,
       ucc.column_name,
       uc.status,
       uc.r_constraint_name
from user_constraints uc
join user_cons_columns ucc
  on ucc.constraint_name = uc.constraint_name
where upper(ucc.column_name) like '%LEGACY%ID%'
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
prompt - *_LEGACY_ID columns and ERP_MASTER_DATA_REFERENCE: do not drop before Step 21.12 cutover PASS.
prompt - *_CANONICAL_ID columns are not legacy by themselves; retain if they are the target model.
prompt - Review every result before generating destructive DDL.
