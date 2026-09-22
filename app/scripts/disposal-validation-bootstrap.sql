-- Test-only account in the isolated disposal validation database. Never run in production.
\set ON_ERROR_STOP on
SELECT current_database() LIKE 'wr_disposal_015_%' AS isolated \gset
\if :isolated
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO erp_organization(version,created_at,updated_at,name,nature,public_organization,active)
VALUES(0,now(),now(),'Disposal validation bootstrap','Validation',false,true) RETURNING id AS org \gset
INSERT INTO erp_person(version,created_at,updated_at,person_type,full_name,active)
VALUES(0,now(),now(),'INDIVIDUAL','Disposal validation operator',true) RETURNING id AS person \gset
INSERT INTO erp_system_user(version,created_at,updated_at,person_id,login,password_hash,mfa_enabled,blocked)
VALUES(0,now(),now(),:person,'disposal-validation',crypt('Disposal-validation-015',gen_salt('bf',12)),false,false) RETURNING id AS account \gset
INSERT INTO erp_access_profile(version,created_at,updated_at,name,level)
VALUES(0,now(),now(),'Disposal validation administrator','SYSTEM') RETURNING id AS profile \gset
INSERT INTO erp_permission(version,created_at,updated_at,resource,action)
VALUES(0,now(),now(),'*','*') ON CONFLICT(resource,action) DO NOTHING;
INSERT INTO erp_profile_permission(version,created_at,updated_at,profile_id,permission_id)
SELECT 0,now(),now(),:profile,id FROM erp_permission WHERE resource='*' AND action='*';
INSERT INTO erp_user_profile(version,created_at,updated_at,user_id,profile_id,organization_id)
VALUES(0,now(),now(),:account,:profile,:org);
COMMIT;
\else
\echo Refusing to bootstrap outside wr_disposal_015_*.
\quit
\endif
