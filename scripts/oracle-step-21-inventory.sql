set pagesize 200
set linesize 220
set trimspool on
set feedback on
set verify off
whenever sqlerror exit sql.sqlcode

alter session set container = FREEPDB1;
set serveroutput on size unlimited

prompt === ETAPA 21 - INVENTARIO ORACLE (SOMENTE LEITURA) ===
select sys_context('USERENV','CON_NAME') as pdb, sys_context('USERENV','SESSION_USER') as session_user from dual;
select owner, count(*) total_tables from dba_tables where owner = 'COMANDOS' and (table_name like 'ERP_%' or table_name like 'FM_%') group by owner;

prompt === CONTAGEM DE TODAS AS TABELAS ERP_/FM_ ===
declare
  n number;
begin
  for t in (select table_name from dba_tables where owner = 'COMANDOS' and (table_name like 'ERP_%' or table_name like 'FM_%') order by table_name) loop
    execute immediate 'select count(*) from "COMANDOS"."' || replace(t.table_name, '"', '""') || '"' into n;
    dbms_output.put_line(rpad(t.table_name, 52) || to_char(n));
  end loop;
end;
/

prompt === RESTRICOES INVALIDAS OU DESABILITADAS ===
select table_name, constraint_name, constraint_type, status, validated from dba_constraints
where owner = 'COMANDOS' and (status <> 'ENABLED' or validated <> 'VALIDATED')
order by table_name, constraint_name;

prompt === OBJETOS INVALIDOS ===
select object_type, object_name, status from dba_objects where owner = 'COMANDOS' and status <> 'VALID' order by object_type, object_name;

prompt === COLUNAS DE CORRESPONDENCIA / LEGADO ===
select table_name, column_name, data_type from dba_tab_columns
where owner = 'COMANDOS' and (column_name like '%LEGACY%' or column_name like '%CANONICAL%' or column_name like '%CROSSWALK%')
order by table_name, column_id;

prompt === RESUMO DE TABELAS DE CADASTRO MESTRE ===
select 'ERP_PERSON' tabela, count(*) total from COMANDOS.ERP_PERSON
union all select 'FM_PERSON', count(*) from COMANDOS.FM_PERSON
union all select 'ERP_ORGANIZATION', count(*) from COMANDOS.ERP_ORGANIZATION
union all select 'FM_ORGANIZATION', count(*) from COMANDOS.FM_ORGANIZATION;

prompt === FIM DO INVENTARIO ===
exit success
