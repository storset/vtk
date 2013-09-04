
drop sequence deleted_resource_seq_pk;

create sequence deleted_resource_seq_pk INCREMENT 1 START 1000;

drop table deleted_resource CASCADE;

create table deleted_resource
(
  id int not null,
  resource_trash_uri varchar (2048) not null,
  parent_id int not null,
  deleted_by varchar(64) not null,
  deleted_time timestamp not null,
  was_inherited_acl CHAR(1) default 'N' not null
);

alter table deleted_resource
  add constraint deleted_resource_PK primary key (id);