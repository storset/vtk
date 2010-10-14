
drop sequence deleted_resource_seq_pk;

create sequence deleted_resource_seq_pk increment by 1 start with 1000;

drop table deleted_resource cascade constraints;

create table deleted_resource
(
  id number not null
);