-- Use property 'description' as 'introduction' for collections that have no 'introduction', but have 'description'

insert into extra_prop_entry
  select nextval('extra_prop_entry_seq_pk'),
    e.resource_id,
    0,
    null,
    'introduction',
    e.value,
    null,
    null
  from extra_prop_entry e
    where e.name = 'description' and e.resource_id in (select resource_id from vortex_resource 
      where resource_id not in (select distinct(resource_id) from extra_prop_entry where name = 'introduction')
      and (resource_type = 'collection' or resource_type = 'article-listing' or resource_type = 'event-listing'));

commit;