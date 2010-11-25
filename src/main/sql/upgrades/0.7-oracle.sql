
drop sequence deleted_resource_seq_pk;

create sequence deleted_resource_seq_pk increment by 1 start with 1000;

drop table deleted_resource cascade constraints;

create table deleted_resource
(
  id number not null,
  resource_trash_uri VARCHAR2 (2048) not null,
  parent_id number not null,
  deleted_by VARCHAR2 (64) not null,
  deleted_time timestamp not null,
  was_inherited_acl char(1) default 'N' not null
);

alter table deleted_resource
  add constraint deleted_resource_PK primary key (id);

alter table deleted_resource
  add constraint deleted_resource_uri_FK foreign key (resource_trash_uri)
  references vortex_resource (uri) on delete cascade;

alter table deleted_resource
  add constraint deleted_resource_parent_FK foreign key (parent_id)
  references vortex_resource (resource_id) on delete cascade;