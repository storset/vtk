-- Insert property hasBodyContent = true for all HTML resources:

insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk'),
       resource_id,
       4,
       null,
       'hasBodyContent',
       'true'
from vortex_resource
where resource_type in ('html', 'xhtml10trans', 'article', 'event');
