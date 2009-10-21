-- Make sure all previously entered published and publish-date properties are removed first

delete from extra_prop_entry where name = 'published' or name = 'publish-date';

-- Add property 'published' to all resources, with value false for structured resources (json) and 
-- value true + published-date (creation_time) for all 'legacy' resources

insert into extra_prop_entry
  select nextval('extra_prop_entry_seq_pk'),
    r.resource_id,
    0,
    null,
    'published',
    'true',
    null,
    null
  from vortex_resource r;

insert into extra_prop_entry
  select nextval('extra_prop_entry_seq_pk'),
    r.resource_id,
    0,
    null,
    'publish-date',
    r.creation_time,
    null,
    null
  from vortex_resource r;

commit;
