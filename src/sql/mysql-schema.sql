-----------------------------------------------------------------------------
-- VORTEX DDL $Id$
-----------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- resource
-----------------------------------------------------------------------------

DROP SEQUENCE vortex_resource_seq_pk;

CREATE SEQUENCE vortex_resource_seq_pk INCREMENT 1 START 1000;

DROP TABLE vortex_resource CASCADE;

CREATE TABLE vortex_resource
(
    resource_id int NOT NULL,
    uri VARCHAR (2048) NOT NULL,
    creation_time TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    modified_by VARCHAR (64) NOT NULL,
    resource_owner VARCHAR (64) NOT NULL,
    display_name VARCHAR (128) NULL,
    content_language VARCHAR (64) NOT NULL,
    content_type VARCHAR (64) NULL,
    is_collection CHAR(1) DEFAULT 'N' NOT NULL,
    acl_inherited CHAR(1) DEFAULT 'Y' NOT NULL,
    CONSTRAINT resource_uri_index UNIQUE (uri)
);

ALTER TABLE vortex_resource
    ADD CONSTRAINT vortex_resource_PK
PRIMARY KEY (resource_id);

-----------------------------------------------------------------------------
-- parent_child
-----------------------------------------------------------------------------
DROP SEQUENCE parent_child_seq_pk;

CREATE SEQUENCE parent_child_seq_pk INCREMENT 1 START 1000;

DROP TABLE parent_child CASCADE;

CREATE TABLE parent_child
(
    parent_child_id int NOT NULL,
    parent_resource_id int NOT NULL,
    child_resource_id int NOT NULL,
    CONSTRAINT parent_child_unique1_index UNIQUE (parent_resource_id, child_resource_id)
);

ALTER TABLE parent_child
    ADD CONSTRAINT parent_child_PK
PRIMARY KEY (parent_child_id);

ALTER TABLE parent_child
    ADD CONSTRAINT parent_child_FK_1 FOREIGN KEY (parent_resource_id)
    REFERENCES vortex_resource (resource_id)
;

ALTER TABLE parent_child
    ADD CONSTRAINT parent_child_FK_2 FOREIGN KEY (child_resource_id)
    REFERENCES vortex_resource (resource_id)
;

-----------------------------------------------------------------------------
-- lock_type
-----------------------------------------------------------------------------
DROP TABLE lock_type CASCADE;

CREATE TABLE lock_type
(
    lock_type_id int NOT NULL,
    name VARCHAR (64) NOT NULL
);

ALTER TABLE lock_type
    ADD CONSTRAINT lock_type_PK
PRIMARY KEY (lock_type_id);


-----------------------------------------------------------------------------
-- lock
-----------------------------------------------------------------------------
DROP SEQUENCE vortex_lock_seq_pk;

CREATE SEQUENCE vortex_lock_seq_pk INCREMENT 1 START 1000;

DROP TABLE vortex_lock CASCADE;

CREATE TABLE vortex_lock
(
    lock_id int NOT NULL,
    resource_id int NOT NULL,
    token VARCHAR (128) NOT NULL,
    lock_type_id int NOT NULL,
    lock_owner VARCHAR (64) NOT NULL,
    lock_owner_info VARCHAR (64) NOT NULL,
    depth CHAR (1) DEFAULT '1' NOT NULL,
    timeout TIMESTAMP NOT NULL
);

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_PK
PRIMARY KEY (lock_id);

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id)
;

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_FK_2 FOREIGN KEY (lock_type_id)
    REFERENCES lock_type (lock_type_id)
;

-----------------------------------------------------------------------------
-- action_type
-----------------------------------------------------------------------------
DROP TABLE action_type CASCADE;

CREATE TABLE action_type
(
    action_type_id int NOT NULL,
    namespace VARCHAR (64) NOT NULL,
    name VARCHAR (64) NOT NULL
);

ALTER TABLE action_type
    ADD CONSTRAINT action_type_PK
PRIMARY KEY (action_type_id);


-----------------------------------------------------------------------------
-- acl_entry
-----------------------------------------------------------------------------
DROP SEQUENCE acl_entry_seq_pk;

CREATE SEQUENCE acl_entry_seq_pk INCREMENT 1 START 1000;

DROP TABLE acl_entry CASCADE;

CREATE TABLE acl_entry
(
    acl_entry_id int NOT NULL,
    resource_id int NOT NULL,
    action_type_id int NOT NULL,
    user_or_group_name VARCHAR (64) NOT NULL,
    is_user CHAR (1) DEFAULT 'Y' NOT NULL,
    granted_by_user_name VARCHAR (64) NOT NULL,
    granted_date TIMESTAMP NOT NULL
);

ALTER TABLE acl_entry
    ADD CONSTRAINT acl_entry_PK
PRIMARY KEY (acl_entry_id);

ALTER TABLE acl_entry
    ADD CONSTRAINT acl_entry_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id)
;

ALTER TABLE acl_entry
    ADD CONSTRAINT acl_entry_FK_2 FOREIGN KEY (action_type_id)
    REFERENCES action_type (action_type_id)
;


-----------------------------------------------------------------------------
-- extra_prop_entry
-----------------------------------------------------------------------------
DROP SEQUENCE extra_prop_entry_seq_pk;

CREATE SEQUENCE extra_prop_entry_seq_pk INCREMENT 1 START 1000;

DROP TABLE extra_prop_entry CASCADE;

CREATE TABLE extra_prop_entry
(
    extra_prop_entry_id int NOT NULL,
    resource_id int NOT NULL,
    name_space VARCHAR (128) NOT NULL,
    name VARCHAR (64) NOT NULL,
    value VARCHAR (2048) NOT NULL
);

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_PK
PRIMARY KEY (extra_prop_entry_id);

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id)
;



-----------------------------------------------------------------------------
-- initial application data
-----------------------------------------------------------------------------

INSERT INTO action_type (action_type_id, namespace, name) VALUES (1, 'dav', 'read');
INSERT INTO action_type (action_type_id, namespace, name) VALUES (2, 'dav', 'write');
INSERT INTO action_type (action_type_id, namespace, name) VALUES (3, 'dav', 'write-acl');
INSERT INTO action_type (action_type_id, namespace, name) VALUES (4, 'uio', 'read-processed');

INSERT INTO LOCK_TYPE (lock_type_id, name) 
VALUES (1, 'EXCLUSIVE_WRITE');

-- root resource

INSERT INTO VORTEX_RESOURCE (
    resource_id,
    uri,
    creation_time,
    last_modified,
    modified_by,
    resource_owner,
    display_name,
    content_language,
    content_type,
    is_collection,
    acl_inherited)
VALUES (
    nextval('vortex_resource_seq_pk'),
    '/',
    current_timestamp,
    current_timestamp,
    'vortex',
    'vortex',
    '/',
    'unknown',
    'application/x-vortex-collection',
    'Y',
    'N'
);


-- (dav:authenticated (dav:read))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    nextval('acl_entry_seq_pk'),
    currval('vortex_resource_seq_pk'),
    1,
    'dav:authenticated',
    'Y',
    'vortex',
    current_timestamp
);    


-- (dav:all (uio:read-processed))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    nextval('acl_entry_seq_pk'),
    currval('vortex_resource_seq_pk'),
    4,
    'dav:all',
    'Y',
    'vortex',
    current_timestamp
);    


-- (dav:owner (dav:read))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    nextval('acl_entry_seq_pk'),
    currval('vortex_resource_seq_pk'),
    1,
    'dav:owner',
    'Y',
    'vortex',
    current_timestamp
);    

-- (dav:owner (dav:write))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    nextval('acl_entry_seq_pk'),
    currval('vortex_resource_seq_pk'),
    2,
    'dav:owner',
    'Y',
    'vortex',
    current_timestamp
);    

-- (dav:owner (dav:write-acl))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    nextval('acl_entry_seq_pk'),
    currval('vortex_resource_seq_pk'),
    3,
    'dav:owner',
    'Y',
    'vortex',
    current_timestamp
);    
