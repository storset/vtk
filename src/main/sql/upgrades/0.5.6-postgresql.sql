-- Use cascading deletes for all child-tables of vortex_resource:

alter table vortex_lock drop constraint vortex_lock_FK_1;
alter table vortex_lock
     add constraint vortex_lock_FK_1 foreign key (resource_id)
     references vortex_resource (resource_id) on delete cascade;


alter table acl_entry drop constraint acl_entry_FK_1;
alter table acl_entry
     add constraint acl_entry_FK_1 foreign key (resource_id)
    references vortex_resource (resource_id) on delete cascade ;


alter table extra_prop_entry drop constraint extra_prop_entry_FK_1;
alter table extra_prop_entry
     add constraint extra_prop_entry_FK_1 foreign key (resource_id)
     references vortex_resource (resource_id) on delete cascade ;




-- Insert property 'visual-profile:disabled = true' for
-- resources that do not have 'visual-profile:enabled = true':

insert into extra_prop_entry 
       (extra_prop_entry_id,
       resource_id,
       prop_type_id,
       name_space,
       name,
       value)
select nextval('extra_prop_entry_seq_pk'),
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
