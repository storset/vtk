-----------------------------------------------------------------------------
-- simple_content_revision
-----------------------------------------------------------------------------

drop sequence simple_content_revision_seq_pk;

create sequence simple_content_revision_seq_pk increment by 1 start with 1000;

drop table simple_content_revision;

create table simple_content_revision (
    id number not null,
    resource_id number not null,
    revision_name varchar2 (256) not null,
    user_id varchar2 (256) not null,
    timestamp timestamp not null,
    checksum varchar2 (256) not null,
    change_amount number
);

alter table simple_content_revision
      add constraint content_revision_pk primary key (id);

alter table simple_content_revision
      add constraint content_revision_fk foreign key (resource_id)
          references vortex_resource (resource_id) on delete cascade;


-----------------------------------------------------------------------------
-- revision_acl_entry
-----------------------------------------------------------------------------
drop sequence revision_acl_entry_seq_pk;

create sequence revision_acl_entry_seq_pk increment by 1 start with 1000;

drop table revision_acl_entry cascade;

create table revision_acl_entry
(
    id number not null,
    revision_id number not null,
    action_type_id number not null,
    user_or_group_name varchar2 (64) not null,
    is_user char (1) default 'Y' not null,
    granted_by_user_name varchar2 (64) not null,
    granted_date date not null
);

alter table revision_acl_entry
    add constraint revision_acl_entry_PK
primary key (id);

alter table revision_acl_entry
    add constraint revision_acl_entry_FK_1 foreign key (revision_id)
    references simple_content_revision (id) on delete cascade;
