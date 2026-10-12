set pagesize 500
set linesize 220
set trimspool on
set feedback on
set verify off
set serveroutput on size unlimited
whenever sqlerror exit sql.sqlcode

prompt ============================================================
prompt COMANDOS STEP 21 - AUTOMATED READ-ONLY ORACLE AUDIT
prompt Target: FREEPDB1 / COMANDOS. No DDL or DML.
prompt ============================================================

alter session set container = FREEPDB1;

column con_name format a20
select sys_context('USERENV','CON_NAME') as con_name from dual;

prompt [1] ERP and FM table inventory
select case when table_name like 'ERP_%' then 'ERP' else 'FM' end as family,
       count(*) as table_count
from dba_tables
where owner='COMANDOS'
  and (table_name like 'ERP_%' or table_name like 'FM_%')
group by case when table_name like 'ERP_%' then 'ERP' else 'FM' end
order by family;

prompt [2] Counts for all ERP and FM tables
declare
    n number;
    total_erp number := 0;
    total_fm number := 0;
    populated number := 0;
    count_errors number := 0;
begin
    for t in (select table_name from dba_tables
              where owner='COMANDOS'
                and (table_name like 'ERP_%' or table_name like 'FM_%')
              order by table_name) loop
        begin
            execute immediate 'select count(*) from COMANDOS."' || replace(t.table_name,'"','""') || '"' into n;
            if t.table_name like 'ERP_%' then total_erp := total_erp+n;
            else total_fm := total_fm+n; end if;
            if n > 0 then populated := populated + 1; end if;
            dbms_output.put_line(rpad(t.table_name,58) || to_char(n));
        exception when others then
            count_errors := count_errors + 1;
            dbms_output.put_line('COUNT_ERROR '|| t.table_name || ': ' || sqlerrm);
        end;
    end loop;
    dbms_output.put_line('ERP_TOTAL_ROWS='||total_erp);
    dbms_output.put_line('FM_TOTAL_ROWS='||total_fm);
    dbms_output.put_line('POPULATED_TABLES='||populated);
    dbms_output.put_line('COUNT_ERRORS='||count_errors);
end;
/

prompt [3] Crosswalk or mapping-related tables and columns
select table_name from dba_tables
where owner='COMANDOS'
  and (table_name like '%CROSSWALK%' or table_name like '%MASTER%DATA%' or table_name like '%MAPPING%')
order by table_name;

select table_name,column_name,data_type from dba_tab_columns
where owner='COMANDOS'
  and (column_name like '%LEGACY%' or column_name like '%CANONICAL%')
order by table_name,column_id;

prompt [4] Invalid constraints and invalid schema objects
select constraint_type,status,validated,count(*) as total
from dba_constraints where owner='COMANDOS'
group by constraint_type,status,validated
order by constraint_type,status,validated;

select object_type,object_name,status
from dba_objects where owner='COMANDOS' and status <> 'VALID'
order by object_type,object_name;

prompt [5] Key person/organization counts
select 'ERP_PERSON' as table_name,count(*) as total from COMANDOS.ERP_PERSON
union all select 'FM_PERSON',count(*) from COMANDOS.FM_PERSON
union all select 'ERP_ORGANIZATION',count(*) from COMANDOS.ERP_ORGANIZATION
union all select 'FM_ORGANIZATION',count(*) from COMANDOS.FM_ORGANIZATION;

prompt [6] Database-only verification is NOT runtime cutover approval
prompt Check Java readiness/parity and reference parity separately.
exit
