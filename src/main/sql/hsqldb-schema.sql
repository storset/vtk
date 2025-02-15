-----------------------------------------------------------------------------
-- VORTEX DDL 
-----------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- vortex_resource
-----------------------------------------------------------------------------

DROP SEQUENCE vortex_resource_seq_pk IF EXISTS;

CREATE SEQUENCE vortex_resource_seq_pk AS INTEGER START WITH 1000;

DROP TABLE vortex_resource IF EXISTS CASCADE;

CREATE TABLE vortex_resource
(
    resource_id int NOT NULL,
    prev_resource_id int NULL, -- used when copying/moving
    uri VARCHAR (2048) NOT NULL,
    depth int NOT NULL,
    creation_time TIMESTAMP NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    content_last_modified TIMESTAMP NOT NULL,
    properties_last_modified TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    content_modified_by VARCHAR (64) NOT NULL,
    properties_modified_by VARCHAR (64) NOT NULL,
    modified_by VARCHAR (64) NOT NULL,
    resource_owner VARCHAR (64) NOT NULL,
    content_language VARCHAR (64) NULL,
    content_type VARCHAR (64) NULL,
    content_length bigint NULL, -- NULL for collections.
    resource_type VARCHAR(64) NOT NULL,
    character_encoding VARCHAR (64) NULL,
    guessed_character_encoding VARCHAR (64) NULL,
    user_character_encoding VARCHAR (64) NULL,
    is_collection CHAR(1) DEFAULT 'N' NOT NULL,
    acl_inherited_from int NULL,
    CONSTRAINT resource_uri_index UNIQUE (uri)
);


ALTER TABLE vortex_resource
      ADD CONSTRAINT vortex_resource_PK PRIMARY KEY (resource_id);


ALTER TABLE vortex_resource
      ADD CONSTRAINT vortex_resource_FK FOREIGN KEY (acl_inherited_from)
          REFERENCES vortex_resource (resource_id);

CREATE INDEX vortex_resource_acl_index ON vortex_resource(acl_inherited_from);

CREATE INDEX vortex_resource_depth_index ON vortex_resource(depth);


-----------------------------------------------------------------------------
-- vortex_tmp - Auxiliary temp-table used to hold lists of URIs or resource-
--              IDs

-- TODO: column 'resource_id' should be renamed to 'generic_id'
-----------------------------------------------------------------------------
DROP SEQUENCE vortex_tmp_session_id_seq IF EXISTS;
CREATE SEQUENCE vortex_tmp_session_id_seq AS INTEGER START WITH 1000;

DROP TABLE vortex_tmp IF EXISTS;
CREATE TABLE vortex_tmp (
    session_id INTEGER,
    resource_id INTEGER,
    uri VARCHAR(2048)
);

CREATE INDEX vortex_tmp_index ON vortex_tmp(uri);


-----------------------------------------------------------------------------
-- vortex_lock
-----------------------------------------------------------------------------
DROP SEQUENCE vortex_lock_seq_pk IF EXISTS;

CREATE SEQUENCE vortex_lock_seq_pk AS INTEGER START WITH 1000;

DROP TABLE vortex_lock IF EXISTS CASCADE;

CREATE TABLE vortex_lock
(
    lock_id int NOT NULL,
    resource_id int NOT NULL,
    token VARCHAR (128) NOT NULL,
    lock_owner VARCHAR (128) NOT NULL,
    lock_owner_info VARCHAR (128) NOT NULL,
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

CREATE INDEX vortex_lock_index1 ON vortex_lock(resource_id);
CREATE INDEX vortex_lock_index2 ON vortex_lock(timeout);

-----------------------------------------------------------------------------
-- action_type
-----------------------------------------------------------------------------
DROP TABLE action_type IF EXISTS CASCADE;

CREATE TABLE action_type
(
    action_type_id int NOT NULL,
    name VARCHAR (64) NOT NULL
);

ALTER TABLE action_type
    ADD CONSTRAINT action_type_PK
PRIMARY KEY (action_type_id);


-----------------------------------------------------------------------------
-- acl_entry
-----------------------------------------------------------------------------
DROP SEQUENCE acl_entry_seq_pk IF EXISTS;

CREATE SEQUENCE acl_entry_seq_pk AS INTEGER START WITH 1000;

DROP TABLE acl_entry IF EXISTS CASCADE;

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

CREATE INDEX acl_entry_index1 ON acl_entry(resource_id);


-----------------------------------------------------------------------------
-- prop_type
-----------------------------------------------------------------------------
DROP TABLE prop_type IF EXISTS CASCADE;

CREATE TABLE prop_type
(
    prop_type_id int NOT NULL,
    prop_type_name VARCHAR(64) NOT NULL
);

ALTER TABLE prop_type
    ADD CONSTRAINT prop_type_PK PRIMARY KEY (prop_type_id);

-----------------------------------------------------------------------------
-- extra_prop_entry
-----------------------------------------------------------------------------
DROP SEQUENCE extra_prop_entry_seq_pk IF EXISTS;

CREATE SEQUENCE extra_prop_entry_seq_pk AS INTEGER START WITH 1000;

DROP TABLE extra_prop_entry IF EXISTS CASCADE;

CREATE TABLE extra_prop_entry
(
    extra_prop_entry_id int NOT NULL,
    resource_id int NOT NULL,
    prop_type_id int DEFAULT 0 NOT NULL,
    name_space VARCHAR (128) NULL,
    name VARCHAR (64) NOT NULL,
    value VARCHAR (2048) NOT NULL,
    binary_content LONGVARBINARY,
    binary_mimetype varchar (64)
);

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_PK
PRIMARY KEY (extra_prop_entry_id);

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id)
;

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_FK_2 FOREIGN KEY (prop_type_id)
    REFERENCES prop_type(prop_type_id)
;

CREATE INDEX extra_prop_entry_index1 ON extra_prop_entry(resource_id);

----------------------------------------------------------------------
-- changelog_entry
-----------------------------------------------------------------------------
DROP SEQUENCE changelog_entry_seq_pk IF EXISTS;

CREATE SEQUENCE changelog_entry_seq_pk AS INTEGER START WITH 1000;

DROP TABLE changelog_entry IF EXISTS;

/* The attribute 'uri' can't be longer that 1578 chars (OS-dependent?).     */
/* If bigger -> "ORA-01450: maximum key length exceeded" (caused by index). */
/* Since combined index '(uri, changelog_entry_id)' -> 1500 chars.          */

CREATE TABLE changelog_entry
(
    changelog_entry_id int NOT NULL,
    logger_id int NOT NULL,
    logger_type int NOT NULL,
    operation VARCHAR (128) NULL,
    timestamp TIMESTAMP NOT NULL,
    uri VARCHAR (1500) NOT NULL,
    resource_id int,
    is_collection CHAR(1) DEFAULT 'N' NOT NULL
);

ALTER TABLE changelog_entry
   ADD CONSTRAINT changelog_entry_PK
PRIMARY KEY (changelog_entry_id);

/* DROP INDEX changelog_entry_index1; */

CREATE UNIQUE INDEX changelog_entry_index1
   ON changelog_entry (uri, changelog_entry_id);


-----------------------------------------------------------------------------
-- initial application data
-----------------------------------------------------------------------------

-- Action types

INSERT INTO action_type (action_type_id, name) VALUES (1, 'read');
INSERT INTO action_type (action_type_id, name) VALUES (2, 'write');
INSERT INTO action_type (action_type_id, name) VALUES (3, 'all');
INSERT INTO action_type (action_type_id, name) VALUES (4, 'read-processed');
INSERT INTO action_type (action_type_id, name) VALUES (5, 'bind');

-- Property value types
-- This data currently corresponds to definitions in 
-- vtk.repository.resourcetype.PropertyType
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (0, 'String');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (1, 'Integer');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (2, 'Long');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (3, 'Date');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (4, 'Boolean');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (5, 'Principal');


-- root resource

INSERT INTO VORTEX_RESOURCE (
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
VALUES (
    next value for vortex_resource_seq_pk,
    NULL,
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
    NULL,
    'application/x-vortex-collection',
    NULL,
    'Y',
    NULL,
    NULL,
    'collection'
);

-- Insert title property for root resource:

INSERT INTO extra_prop_entry 
SELECT next value for extra_prop_entry_seq_pk,
       resource_id,
       0,
       null,
       'title',
       '/'
from vortex_resource where uri = '/';


-- (pseudo:all, read)

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    next value for acl_entry_seq_pk,
    1000,
    1,
    'pseudo:all',
    'Y',
    'vortex@localhost',
    current_timestamp
);    


-- (pseudo:owner, all)

INSERT INTO ACL_ENTRY (
    acl_entry_id,
    resource_id,
    action_type_id,
    user_or_group_name,
    is_user,
    granted_by_user_name,
    granted_date)
VALUES (
    next value for acl_entry_seq_pk,
    --nextval('acl_entry_seq_pk'),
    1000,
    --currval('vortex_resource_seq_pk'),
    3,
    'pseudo:owner',
    'Y',
    'vortex@localhost',
    current_timestamp
);    

