-- Map old permissions to new:
update action_type set name = 'read-write' where action_type_id = 2;
update action_type set name = 'bind-template' where action_type_id = 5;


-- Not for production databases

-- Resolve ACL entries with 'pseudo:owner' and pseudo:authenticated
update acl_entry a
set user_or_group_name =
   (select resource_owner
   from vortex_resource r
   where r.resource_id = a.resource_id)
where a.user_or_group_name = 'pseudo:owner';

update acl_entry a
set user_or_group_name =
  (select resource_owner
  from vortex_resource r
  where r.resource_id = a.resource_id)
where a.user_or_group_name = 'pseudo:authenticated';

delete from acl_entry 
where action_type_id = 5;

