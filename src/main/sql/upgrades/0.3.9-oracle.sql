-- Delete existing htmlTitle properties for XHTML resources
delete from extra_prop_entry where extra_prop_entry_id in
       (select extra_prop_entry_id from extra_prop_entry p, vortex_resource r
        where r.resource_type = 'xhtml10trans'
              and r.resource_id = p.resource_id
              and p.name_space is null 
              and p.name = 'htmlTitle');

-- Insert new htmlTitle properties (based on existing xhtmlTitle):
insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk'),
   p.resource_id,
   0,
   null,
   'htmlTitle',
   p.value
from extra_prop_entry p, vortex_resource r
where r.resource_type = 'xhtml10trans'
      and r.resource_id = p.resource_id
      and p.name_space is null
      and p.name = 'xhtmlTitle';

-- Delete xhtmlTitle properties:
delete from extra_prop_entry where extra_prop_entry_id in
       (select extra_prop_entry_id from extra_prop_entry p, vortex_resource r
       where r.resource_type = 'xhtml10trans'
             and r.resource_id = p.resource_id
             and p.name_space is null
             and p.name = 'xhtmlTitle');
      
