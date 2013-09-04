/* Insert value for display aggregation property for all resources that have aggregation property set */

delete from extra_prop_entry where name_space is null and name = 'display-aggregation';

insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk'),
       resource_id,
       0,
       null,
       'display-aggregation',
       'true',
       null,
       null
from vortex_resource 
where resource_id in
    (select distinct(resource_id) from extra_prop_entry
        where name_space is null and name = 'aggregation');