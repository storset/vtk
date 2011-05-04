-- sqlplus formatting
set linesize 32000;
set wrap on;
set echo on;
set heading off;
set pagesize 0;

-- Map old permissions to new:
update action_type set name = 'read-write' where action_type_id = 2;
update action_type set name = 'bind-template' where action_type_id = 5;


-- Rapporter:

select '**SCHEMA** ' || sys_context('USERENV', 'CURRENT_SCHEMA')
from dual;

select 'owner_acl', a.resource_id, r.uri
from acl_entry a, vortex_resource r
where a.user_or_group_name = 'pseudo:owner'
and a.resource_id = r.resource_id
and not exists 
   (select a2.action_type_id
   from acl_entry a2 
   where a2.resource_id = a.resource_id
   and   a2.user_or_group_name <> 'pseudo:owner'
   and   a2.action_type_id = 3)
;
      
select 'pseudo_auth_feide', ae2.resource_id, r.uri
from acl_entry ae2, vortex_resource r
where ae2.user_or_group_name = 'pseudo:authenticated'
and ae2.resource_id = r.resource_id
and sys_context('USERENV', 'CURRENT_SCHEMA') in ('WWW_OKONOMI_UIO_NO')
; 

select distinct 'pseudo_auth_alle', ae2.resource_id, r.uri
from acl_entry ae2, vortex_resource r
where ae2.user_or_group_name = 'pseudo:authenticated'
and ae2.resource_id = r.resource_id
; 
   
select distinct 'bind-template', ae2.resource_id, r.uri
from acl_entry ae2, vortex_resource r
where ae2.resource_id = r.resource_id
and ae2.action_type_id = 5
;

select 'dup', a.resource_id, a.action_type_id, a.is_user, a.user_or_group_name, r.uri
from acl_entry a, vortex_resource r
WHERE 
  a.resource_id = r.resource_id
  and a.rowid > 
   ANY (
     SELECT B.rowid
     FROM acl_entry B
     WHERE A.resource_id = B.resource_id
     AND   A.action_type_id = B.action_type_id
     AND   A.is_user = B.is_user
     AND   A.user_or_group_name = B.user_or_group_name
       )
;

select 'read_all_delete', ae.resource_id, ae.action_type_id, ae.is_user, ae.user_or_group_name, r.uri
from acl_entry ae, vortex_resource r
where ae.action_type_id = 1 -- read
and ae.resource_id = r.resource_id
and not (ae.user_or_group_name = 'pseudo:all' and ae.is_user = 'Y')
and exists 
  (select 'Y'
   from acl_entry ae2
   where ae2.resource_id = ae.resource_id
   and ae2.action_type_id = 1
   and ae2.user_or_group_name = 'pseudo:all' 
   and ae2.is_user = 'Y')
;

select 'read_write_dup', ae.resource_id, ae.action_type_id, ae.is_user, ae.user_or_group_name, r.uri
from acl_entry ae, vortex_resource r
where ae.action_type_id = 1 -- read
and ae.resource_id = r.resource_id
and exists 
  (select 'Y'
   from acl_entry ae2
   where ae2.resource_id = ae.resource_id
   and ae2.action_type_id = 2
   and ae2.user_or_group_name = ae.user_or_group_name 
   and ae2.is_user = ae.is_user)
;

select 'comment_prop', vr.resource_id, vr.uri
from vortex_resource vr
where exists
  (select 'Y'
   from acl_entry ae 
   where ae.action_type_id = 6
   and   ae.resource_id = vr.resource_id)
;   


-- Get rid of pseudo:owner VRTXA-258

-- Add all priv only if no rows with admin for this resource
update acl_entry a
set user_or_group_name =
   (select resource_owner
   from vortex_resource r
   where r.resource_id = a.resource_id)
where a.user_or_group_name = 'pseudo:owner'
and not exists 
   (select a2.action_type_id
   from acl_entry a2 
   where a2.resource_id = a.resource_id
   and   a2.user_or_group_name <> 'pseudo:owner'
   and   a2.action_type_id = 3)
;

delete from acl_entry a
where a.user_or_group_name = 'pseudo:owner'
;

-- Get rid of pseudo:authenticated VRTXA-258
insert into acl_entry (acl_entry_id, resource_id, action_type_id, user_or_group_name,
                       is_user, granted_by_user_name, granted_date)
select acl_entry_seq_pk.nextval, resource_id, action_type_id, 'alle@feide.no',
       'N', granted_by_user_name, granted_date
from acl_entry ae2
where ae2.user_or_group_name = 'pseudo:authenticated'
and sys_context('USERENV', 'CURRENT_SCHEMA') in ('WWW_OKONOMI_UIO_NO')
;                     

update acl_entry a 
set user_or_group_name = 'alle@uio.no',
    is_user = 'N'
where a.user_or_group_name = 'pseudo:authenticated'
;

# Sjekk verdier

-- Remove bind-template for now VRTXA-258
delete from acl_entry 
where action_type_id = 5;

delete from action_type 
where action_type_id = 5;

-- Remove logical duplicates in acl, some added by conversion above VRTXA-258
DELETE FROM 
   acl_entry A
WHERE 
  a.rowid > 
   ANY (
     SELECT B.rowid
     FROM acl_entry B
     WHERE A.resource_id = B.resource_id
     AND   A.action_type_id = B.action_type_id
     AND   A.is_user = B.is_user
     AND   A.user_or_group_name = B.user_or_group_name
       )
;

-- Remove other read aces for read if has (pseudo:all, read) VRTXA-258
delete from acl_entry ae 
where ae.action_type_id = 1 -- read
and not (ae.user_or_group_name = 'pseudo:all' and ae.is_user = 'Y')
and exists 
  (select 'Y'
   from acl_entry ae2
   where ae2.resource_id = ae.resource_id
   and ae2.action_type_id = 1
   and ae2.user_or_group_name = 'pseudo:all' 
   and ae2.is_user = 'Y')
;

-- Remove read if has identical read-write VRTXA-258
delete from acl_entry ae 
where ae.action_type_id = 1 -- read
and exists 
  (select 'Y'
   from acl_entry ae2
   where ae2.resource_id = ae.resource_id
   and ae2.action_type_id = 2
   and ae2.user_or_group_name = ae.user_or_group_name 
   and ae2.is_user = ae.is_user)
;

-- Commenting conversion VRTXA-258
insert into extra_prop_entry 
    (extra_prop_entry_id,
    resource_id,
    prop_type_id,
    name_space,
    name,
    value,
    binary_mimetype)
select extra_prop_entry_seq_pk.nextval,
       vr.resource_id,
       0,
       null,
       'commentsEnabled',
       'true',
       null
from vortex_resource vr
where exists
  (select 'Y'
   from acl_entry ae 
   where ae.action_type_id = 6
   and   ae.resource_id = vr.resource_id)
;   

commit;
exit;
