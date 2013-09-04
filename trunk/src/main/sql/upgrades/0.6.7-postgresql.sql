insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk'),
       resource_id,
       0,
       null,
       'publish-date',
       r.creation_time,
       null,
       null
from vortex_resource 
where resource_id in
    (select resource_id from extra_prop_entry
       where name_space is null and name = 'published' and value = 'true')
and resource_id not in 
    (select resource_id from extra_prop_entry
          where name_space is null and name = 'publish-date');
