-----------------------------------------------------------------------------
-- VORTEX DDL 
-----------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- vortex_resource
-----------------------------------------------------------------------------

drop sequence vortex_resource_seq_pk;

create sequence vortex_resource_seq_pk INCREMENT 1 START 1000;

drop table vortex_resource CASCADE;

create table vortex_resource
(
    resource_id int not null,
    prev_resource_id int null, -- used when copying/moving
    uri varchar (2048) not null,
    depth int not null,
    creation_time timestamp not null,
    created_by varchar(64) not null,
    content_last_modified timestamp not null,
    properties_last_modified timestamp not null,
    last_modified timestamp not null,
    content_modified_by varchar (64) not null,
    properties_modified_by varchar (64) not null,
    modified_by varchar (64) not null,
    resource_owner varchar (64) not null,
    content_language varchar (64) null,
    content_type varchar (128) null,
    content_length bigint null, -- NULL for collections.
    resource_type varchar(64) not null,
    character_encoding varchar (64) null,
    guessed_character_encoding varchar (64) null,
    user_character_encoding varchar (64) null,
    is_collection CHAR(1) default 'N' not null,
    acl_inherited_from int null,
    constraint resource_uri_index unique (uri)
);


alter table vortex_resource
      add constraint vortex_resource_PK primary key (resource_id);

alter table vortex_resource
      add constraint vortex_resource_FK foreign key (acl_inherited_from)
          references vortex_resource (resource_id);

create index vortex_resource_acl_index on vortex_resource(acl_inherited_from);

create index vortex_resource_depth_index on vortex_resource(depth);


-----------------------------------------------------------------------------
-- resource_ancestor_ids
-- Stored function for retrieving a resource's ancestor IDs.
-- Returns a VARCHAR of single-space-separated id integers.
-----------------------------------------------------------------------------
drop function resource_ancestor_ids(varchar); 
create or replace
function resource_ancestor_ids(varchar) returns varchar as ' DECLARE
  parent varchar DEFAULT ''/'';
  id integer;
  ids varchar DEFAULT '''';
  slashpos integer DEFAULT 1;
  nextslash integer DEFAULT -1;
BEGIN
  IF $1 = ''/'' THEN RETURN ids; END IF;

  LOOP
    SELECT INTO id vr.resource_id FROM vortex_resource vr
    WHERE vr.uri = parent AND vr.is_collection = ''Y'';
    ids := ids || '' '' || id;
    nextslash := position(''/'' in substring($1 from slashpos+1 for char_length($1)-slashpos));
    EXIT WHEN nextslash = 0;
    slashpos := slashpos + nextslash;
    parent := substring($1 from 1 for slashpos-1);
  END LOOP;
  RETURN ids;
END;
' language plpgsql;

-----------------------------------------------------------------------------
-- vortex_tmp 
-- Auxiliary temp-table used to hold lists of URIs or resource-IDs

-- TODO: column 'resource_id' should be renamed to 'generic_id'
-----------------------------------------------------------------------------
drop sequence vortex_tmp_session_id_seq;
create sequence vortex_tmp_session_id_seq increment 1 start 1000;

drop table vortex_tmp;
create table vortex_tmp (
    session_id integer,
    resource_id integer,
    uri varchar(2048)
);

create index vortex_tmp_index on vortex_tmp(uri);


-----------------------------------------------------------------------------
-- vortex_lock
-----------------------------------------------------------------------------
drop sequence vortex_lock_seq_pk;

create sequence vortex_lock_seq_pk INCREMENT 1 START 1000;

drop table vortex_lock CASCADE;

create table vortex_lock
(
    lock_id int not null,
    resource_id int not null,
    token varchar (128) not null,
    lock_owner varchar (128) not null,
    lock_owner_info varchar (128) not null,
    depth char (1) default '1' not null,
    timeout timestamp not null
);

alter table vortex_lock
    add constraint vortex_lock_PK
primary key (lock_id);

alter table vortex_lock
    add constraint vortex_lock_FK_1 foreign key (resource_id)
    references vortex_resource (resource_id) on delete cascade
;

create index vortex_lock_index1 on vortex_lock(resource_id);
create index vortex_lock_index2 on vortex_lock(timeout);

-----------------------------------------------------------------------------
-- action_type
-----------------------------------------------------------------------------
drop table action_type cascade;

create table action_type
(
    action_type_id int not null,
    name VARCHAR (64) not null
);

alter table action_type
    add constraint action_type_PK
primary key (action_type_id);


-----------------------------------------------------------------------------
-- acl_entry
-----------------------------------------------------------------------------
drop sequence acl_entry_seq_pk;

create sequence acl_entry_seq_pk increment 1 start 1000;

drop table acl_entry cascade;

create table acl_entry
(
    acl_entry_id int not null,
    resource_id int not null,
    action_type_id int not null,
    user_or_group_name varchar (64) not null,
    is_user char (1) default 'Y' not null,
    granted_by_user_name varchar (64) not null,
    granted_date timestamp not null
);

alter table acl_entry
    add constraint acl_entry_PK
primary key (acl_entry_id);

alter table acl_entry
    add constraint acl_entry_FK_1 foreign key (resource_id)
    references vortex_resource (resource_id) on delete cascade
;

-- Unnecessary constraint, table not really in use:
-- alter table acl_entry
--     add constraint acl_entry_FK_2 foreign key (action_type_id)
--     references action_type (action_type_id)
-- ;

create index acl_entry_index1 on acl_entry(resource_id);


-----------------------------------------------------------------------------
-- prop_type
-----------------------------------------------------------------------------
drop table prop_type cascade;

create table prop_type
(
    prop_type_id int not null,
    prop_type_name varchar(64) not null
);

alter table prop_type
    add constraint prop_type_PK primary key (prop_type_id);


-----------------------------------------------------------------------------
-- extra_prop_entry
-----------------------------------------------------------------------------
drop sequence extra_prop_entry_seq_pk;

create sequence extra_prop_entry_seq_pk increment 1 start 1000;

drop table extra_prop_entry cascade;

create table extra_prop_entry
(
    extra_prop_entry_id int not null,
    resource_id int not null,
    prop_type_id int default 0 not null,
    name_space varchar (128) null,
    name varchar (64) not null,
    value varchar (2048) not null,
    binary_content oid,
    binary_mimetype varchar (64)
);

ALTER TABLE extra_prop_entry
    add constraint extra_prop_entry_PK
primary key (extra_prop_entry_id);

alter table extra_prop_entry
    add constraint extra_prop_entry_FK_1 foreign key (resource_id)
    references vortex_resource (resource_id) on delete cascade
;

-- Unnecessary constraint, table not really in use:
-- alter table extra_prop_entry
--     add constraint extra_prop_entry_FK_2 foreign key (prop_type_id)
--     references prop_type(prop_type_id)
-- ;

create index extra_prop_entry_index1 on extra_prop_entry(resource_id);


-----------------------------------------------------------------------------
-- changelog_entry
-----------------------------------------------------------------------------
drop sequence changelog_entry_seq_pk;

create sequence changelog_entry_seq_pk increment 1 start 1000;

drop table changelog_entry;

create table changelog_entry
(
    changelog_entry_id int not null,
    logger_id int not null,
    logger_type int not null,
    operation varchar (128) null,
    timestamp timestamp not null,
    uri varchar (2048) not null,
    resource_id int,
    is_collection char(1) default 'N' not null
);

alter table changelog_entry
   add constraint changelog_entry_PK
primary key (changelog_entry_id);


/* DROP INDEX changelog_entry_index1; */

create unique index changelog_entry_index1
   on changelog_entry (uri, changelog_entry_id);


-----------------------------------------------------------------------------
-- vortex_comment
-----------------------------------------------------------------------------
drop sequence vortex_comment_seq_pk;

create sequence vortex_comment_seq_pk increment 1 start 1000;

drop table vortex_comment;

create table vortex_comment
(
    id int not null,
    resource_id int not null,
    author varchar(64) not null,
    time timestamp not null,
    title varchar(2048) null,
    content text not null,
    approved char(1) default 'Y' not null
);

alter table vortex_comment
      add constraint vortex_comment_pk primary key (id);

alter table vortex_comment
      add constraint vortex_comment_fk foreign key (resource_id)
          references vortex_resource (resource_id) on delete cascade;

create index vortex_comment_index1 on vortex_comment(resource_id);


-----------------------------------------------------------------------------
-- initial application data
-----------------------------------------------------------------------------

-- Action types

insert into action_type (action_type_id, name) values (1, 'read');
insert into action_type (action_type_id, name) values (2, 'read-write');
insert into action_type (action_type_id, name) values (3, 'all');
insert into action_type (action_type_id, name) values (4, 'read-processed');
--insert into action_type (action_type_id, name) values (5, 'bind-template');
insert into action_type (action_type_id, name) values (6, 'add-comment');

-- Property value types
-- This data currently corresponds to definitions in 
-- org.vortikal.repository.resourcetype.PropertyType
insert into prop_type (prop_type_id, prop_type_name) values (0, 'String');
insert into prop_type (prop_type_id, prop_type_name) values (1, 'Integer');
insert into prop_type (prop_type_id, prop_type_name) values (2, 'Long');
insert into prop_type (prop_type_id, prop_type_name) values (3, 'Date');
insert into prop_type (prop_type_id, prop_type_name) values (4, 'Boolean');
insert into prop_type (prop_type_id, prop_type_name) values (5, 'Principal');

-- root resource

insert into vortex_resource (
    resource_id,
    prev_resource_id,
    uri,
    depth,
    creation_time,
    created_by,
    content_last_modified,
    properties_last_modified,
    last_modified,
    content_modified_by,
    properties_modified_by,
    modified_by,
    resource_owner,
    content_language,
    content_type,
    character_encoding,
    is_collection,
    acl_inherited_from,
    content_length,
    resource_type)
values (
    nextval('vortex_resource_seq_pk'),
    null,
    '/',
    0,
    current_timestamp,
    'vortex@localhost',
    current_timestamp,
    current_timestamp,
    current_timestamp,
    'vortex@localhost',
    'vortex@localhost',
    'vortex@localhost',
    'vortex@localhost',
    null,
    null,
    null,
    'Y',
    null,
    null,
    'collection'
);

-- Insert title property for root resource:

insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk'),
       resource_id,
       0,
       null,
       'title',
       '/',
       null,
       null
from vortex_resource where uri = '/';


-- (pseudo:all, read)

insert into acl_entry (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
values (
    nextval('acl_entry_seq_pk'),
    currval('vortex_resource_seq_pk'),
    1,
    'pseudo:all',
    'Y',
    'vortex@localhost',
    current_timestamp
);


-- (vortex@localhost, all)

insert into ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
values (
    nextval('acl_entry_seq_pk'),
    currval('vortex_resource_seq_pk'),
    3,
    'vortex@localhost',
    'Y',
    'vortex@localhost',
    current_timestamp
);

-----------------------------------------------------------------------------
-- deleted_resource (trash can)
-----------------------------------------------------------------------------

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
