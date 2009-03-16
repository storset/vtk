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

