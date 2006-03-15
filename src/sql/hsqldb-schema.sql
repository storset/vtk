-----------------------------------------------------------------------------
-- VORTEX DDL $Id: hsqldb-schema.sql,v 1.7 2003/12/05 12:31:03 gormap Exp $
-----------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- drop old tables
-----------------------------------------------------------------------------

DROP TABLE parent_child IF EXISTS;
DROP TABLE extra_prop_entry IF EXISTS;
DROP TABLE vortex_lock IF EXISTS;
DROP TABLE lock_type IF EXISTS;
DROP TABLE acl_entry IF EXISTS;
DROP TABLE action_type IF EXISTS;
DROP TABLE vortex_resource IF EXISTS;
DROP TABLE changelog_entry IF EXISTS;


-----------------------------------------------------------------------------
-- A nasty hack to make the 'nextval' construct work in HSQLDB. This
-- enables us to use the same SQL in this database as the PostgreSQL
-- database, for instance. As long as no system property is defined
-- that matches any of the identity column names herein, we are
-- fine. In fact, any static java function that takes a string as
-- argument and _always_ returns null will suit our needs. Any
-- suggestions other that System.getProperty() are welcome :)
-----------------------------------------------------------------------

CREATE ALIAS nextval FOR "java.lang.System.getProperty"


-----------------------------------------------------------------------------
-- resource
-----------------------------------------------------------------------------

CREATE TABLE vortex_resource
(
    resource_id IDENTITY NOT NULL PRIMARY KEY,
    prev_resource_id int NULL,
    uri VARCHAR (2048) NOT NULL,
    depth int NOT NULL,
    creation_time DATE NOT NULL,
    content_last_modified DATETIME NOT NULL,
    properties_last_modified DATETIME NOT NULL,
    content_modified_by VARCHAR (64) NOT NULL,
    properties_modified_by VARCHAR (64) NOT NULL,
    resource_owner VARCHAR (64) NOT NULL,
    display_name VARCHAR (128) NULL,
    content_language VARCHAR (64) NULL,
    content_type VARCHAR (64) NULL,
    character_encoding VARCHAR (64) NULL,
    is_collection CHAR(1) DEFAULT 'N' NOT NULL,
    acl_inherited CHAR(1) DEFAULT 'Y' NOT NULL,
    CONSTRAINT resource_uri_index UNIQUE (uri)
);



-----------------------------------------------------------------------------
-- parent_child
-----------------------------------------------------------------------------

CREATE TABLE parent_child
(
    parent_child_id IDENTITY NOT NULL PRIMARY KEY,
    parent_resource_id int NOT NULL,
    child_resource_id int NOT NULL,
    CONSTRAINT parent_child_unique1_index UNIQUE (parent_resource_id, child_resource_id),
    FOREIGN KEY (parent_resource_id) REFERENCES vortex_resource (resource_id),
    FOREIGN KEY (child_resource_id) REFERENCES vortex_resource (resource_id)
);



-----------------------------------------------------------------------------
-- lock_type
-----------------------------------------------------------------------------

CREATE TABLE lock_type
(
    lock_type_id IDENTITY NOT NULL PRIMARY KEY,
    name VARCHAR (64) NOT NULL
);



-----------------------------------------------------------------------------
-- lock
-----------------------------------------------------------------------------

CREATE TABLE vortex_lock
(
    lock_id IDENTITY NOT NULL PRIMARY KEY,
    resource_id int NOT NULL,
    token VARCHAR (128) NOT NULL,
    lock_type_id int NOT NULL,
    lock_owner VARCHAR (128) NOT NULL,
    lock_owner_info VARCHAR (128) NOT NULL,
    depth CHAR (1) DEFAULT '1' NOT NULL,
    timeout DATETIME NOT NULL,
    FOREIGN KEY (resource_id) REFERENCES vortex_resource (resource_id),
    FOREIGN KEY (lock_type_id) REFERENCES lock_type (lock_type_id)
);



-----------------------------------------------------------------------------
-- action_type
-----------------------------------------------------------------------------

CREATE TABLE action_type
(
    action_type_id IDENTITY NOT NULL PRIMARY KEY,
    namespace VARCHAR (64) NOT NULL,
    name VARCHAR (64) NOT NULL
);



-----------------------------------------------------------------------------
-- acl_entry
-----------------------------------------------------------------------------

CREATE TABLE acl_entry
(
    acl_entry_id IDENTITY NOT NULL PRIMARY KEY,
    resource_id int NOT NULL,
    action_type_id int NOT NULL,
    user_or_group_name VARCHAR (64) NOT NULL,
    is_user CHAR (1) DEFAULT 'Y' NOT NULL,
    granted_by_user_name VARCHAR (64) NOT NULL,
    granted_date DATE NOT NULL,
    FOREIGN KEY (resource_id) REFERENCES vortex_resource (resource_id),
    FOREIGN KEY (action_type_id) REFERENCES action_type (action_type_id)
);



-----------------------------------------------------------------------------
-- extra_prop_entry
-----------------------------------------------------------------------------

CREATE TABLE extra_prop_entry
(
    extra_prop_entry_id IDENTITY NOT NULL PRIMARY KEY,
    resource_id int NOT NULL,
    name_space VARCHAR (128) NOT NULL,
    name VARCHAR (64) NOT NULL,
    value VARCHAR (2048) NOT NULL,
    FOREIGN KEY (resource_id) REFERENCES vortex_resource (resource_id)
);



----------------------------------------------------------------------
-- changelog_entry
-----------------------------------------------------------------------------

/* The attribute 'uri' can't be longer that 1578 chars (OS-dependent?).     */
/* If bigger -> "ORA-01450: maximum key length exceeded" (caused by index). */
/* Since combined index '(uri, changelog_entry_id)' -> 1500 chars.          */

CREATE TABLE changelog_entry
(
    changelog_entry_id IDENTITY NOT NULL PRIMARY KEY,
    logger_id int NOT NULL,
    logger_type int NOT NULL,
    operation VARCHAR (128) NULL,
    timestamp DATETIME NOT NULL,
    uri VARCHAR (1500) NOT NULL,
    resource_id int,
    is_collection CHAR(1) DEFAULT 'N' NOT NULL
);



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
    prev_resource_id,
    uri,
    depth,
    creation_time,
    content_last_modified,
    properties_last_modified,
    content_modified_by,
    properties_modified_by,
    resource_owner,
    display_name,
    content_language,
    content_type,
    character_encoding,
    is_collection,
    acl_inherited)
VALUES (
    1,
    null,
    '/',
    0,
    current_timestamp,
    current_timestamp,
    current_timestamp,
    'vortex@localhost',
    'vortex@localhost',
    'vortex@localhost',
    '/',
    null,
    'application/x-vortex-collection',
    null,
    'Y',
    'N'
);



-- ACE: (dav:authenticated (dav:read))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    1,
    1,
    1,
    'dav:authenticated',
    'Y',
    'vortex@localhost',
    now()
);    

-- ACE: (dav:all (uio:read-processed))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    2,
    1,
    4,
    'dav:all',
    'Y',
    'vortex@localhost',
    now()
);    


-- ACE: (dav:owner (dav:read))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    3,
    1,
    1,
    'dav:owner',
    'Y',
    'vortex@localhost',
    now()
);    


-- ACE: (dav:owner (dav:write))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    4,
    1,
    2,
    'dav:owner',
    'Y',
    'vortex@localhost',
    now()
);    


-- ACE: (dav:owner (dav:write-acl))

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    5,
    1,
    3,
    'dav:owner',
    'Y',
    'vortex@localhost',
    now()
);    
