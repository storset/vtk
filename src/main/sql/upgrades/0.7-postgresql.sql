
drop sequence deleted_resource_seq_pk;

create sequence deleted_resource_seq_pk INCREMENT 1 START 1000;

drop table deleted_resource CASCADE;

create table deleted_resource
(
  id int not null,
  resource_trash_uri varchar (64) not null,
  parent_id int not null,
  deleted_by varchar(64) not null,
  deleted_time timestamp not null,
  was_inherited_acl CHAR(1) default 'Y' not null
);

alter table deleted_resource
  add constraint deleted_resource_PK primary key (id);

alter table deleted_resource
  add constraint deleted_resource_uri_FK foreign key (resource_trash_uri)
  references vortex_resource (uri) on delete cascade;

alter table deleted_resource
  add constraint deleted_resource_parent_FK foreign key (parent_id)
  references vortex_resource (resource_id) on delete cascade;