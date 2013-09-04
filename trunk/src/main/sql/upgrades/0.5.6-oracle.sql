-- Set ordering requirement and caching on changelog_entry_seq_pk sequence:

alter sequence changelog_entry_seq_pk cache 60 order;

-- Create index on foreign key constraint for vortex_comment:
create index vortex_comment_index1 on vortex_comment(resource_id);

-- Remove prop_type_id foreign key constraint from extra_prop_entry:
alter table extra_prop_entry drop constraint extra_prop_entry_FK_2;

-- Remove action_type_id foreign key constraint from acl_entry:
alter table acl_entry drop constraint acl_entry_FK_2;

-- Insert property 'visual-profile:disabled = true' for
-- resources that do not have 'visual-profile:enabled = true':

insert into extra_prop_entry 
       (extra_prop_entry_id,
       resource_id,
       prop_type_id,
       name_space,
       name,
       value)
select extra_prop_entry_seq_pk.nextval,
       resource_id,
       0,
       'http://www.uio.no/visual-profile',
       'disabled',
       'true'
from vortex_resource
where resource_type in
        ('apt-resource', 'html', 'xhtml10trans', 'document', 'article',
         'event', 'managed-xml', 'emne', 'semester', 'emnegruppe',
         'studieprogram', 'studieretning', 'utvekslingsavtale',
         'artikkelsamling', 'artikkel', 'disputas', 'proveforelesning',
         'nyhet', 'arrangement', 'publikasjon', 'sakskart', 'treaty', 'portal',
         'romskjema', 'muv-artikkel', 'researcher', 'project', 'collection',
         'article-listing', 'event-listing')
and resource_id not in
    (select resource_id from extra_prop_entry 
          where name_space = 'http://www.uio.no/visual-profile'
          and name = 'enabled');

-- Delete property 'visual-profile:enabled = true':

delete from extra_prop_entry
       where name_space = 'http://www.uio.no/visual-profile'
       and name = 'enabled';


-- Commit and exit:
commit;
exit;

-- END
