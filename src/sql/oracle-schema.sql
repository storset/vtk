-----------------------------------------------------------------------------
-- VORTEX DDL $Id: oracle-schema.sql,v 1.12 2004/03/19 17:08:47 gormap Exp $
-- This file contains definitions of the tables required by the
-- OracleDatabase class. 
-----------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- resource
-----------------------------------------------------------------------------

DROP SEQUENCE vortex_resource_seq_pk;

CREATE SEQUENCE vortex_resource_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE vortex_resource CASCADE CONSTRAINTS;

/* The attribute 'uri' can't be longer that 1578 chars (OS-dependent?).     */
/* If bigger -> "ORA-01450: maximum key length exceeded" (caused by index). */
/* Since combined index '(uri, changelog_entry_id)' -> 1500 chars.          */

CREATE TABLE vortex_resource
(
    resource_id NUMBER NOT NULL,
    uri NVARCHAR2 (1500) NOT NULL,
    creation_time DATE NOT NULL,
    content_last_modified DATE NOT NULL,
    properties_last_modified DATE NOT NULL,
    content_modified_by VARCHAR2 (64) NOT NULL,
    properties_modified_by VARCHAR2 (64) NOT NULL,
    resource_owner VARCHAR2 (64) NOT NULL,
    display_name NVARCHAR2 (128) NULL,
    content_language VARCHAR2 (64) NOT NULL,
    content_type VARCHAR2 (64) NULL,
    character_encoding VARCHAR2 (64) NULL,
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

CREATE SEQUENCE parent_child_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE parent_child CASCADE CONSTRAINTS;

CREATE TABLE parent_child
(
    parent_child_id NUMBER NOT NULL,
    parent_resource_id NUMBER NOT NULL,
    child_resource_id NUMBER NOT NULL,
    CONSTRAINT parent_child_unique1_index UNIQUE (parent_resource_id, child_resource_id)
);

ALTER TABLE parent_child
    ADD CONSTRAINT parent_child_PK
PRIMARY KEY (parent_child_id);

ALTER TABLE parent_child
    ADD CONSTRAINT parent_child_FK_1 FOREIGN KEY (parent_resource_id)
    REFERENCES vortex_resource (resource_id) ON DELETE CASCADE
;

ALTER TABLE parent_child
    ADD CONSTRAINT parent_child_FK_2 FOREIGN KEY (child_resource_id)
    REFERENCES vortex_resource (resource_id) ON DELETE CASCADE
;

-- ALTER TABLE parent_child
--     ADD CONSTRAINT parent_child_FK_1 FOREIGN KEY (parent_resource_id)
--     REFERENCES vortex_resource (resource_id)
-- ;

-- ALTER TABLE parent_child
--     ADD CONSTRAINT parent_child_FK_2 FOREIGN KEY (child_resource_id)
--     REFERENCES vortex_resource (resource_id)
-- ;

CREATE INDEX parent_child_index1 ON parent_child (parent_resource_id);

CREATE INDEX parent_child_index2 ON parent_child (child_resource_id);


-----------------------------------------------------------------------------
-- lock_type
-----------------------------------------------------------------------------
DROP TABLE lock_type CASCADE CONSTRAINTS;

CREATE TABLE lock_type
(
    lock_type_id NUMBER NOT NULL,
    name VARCHAR2 (64) NOT NULL
);

ALTER TABLE lock_type
    ADD CONSTRAINT lock_type_PK
PRIMARY KEY (lock_type_id);


-----------------------------------------------------------------------------
-- lock
-----------------------------------------------------------------------------
DROP SEQUENCE vortex_lock_seq_pk;

CREATE SEQUENCE vortex_lock_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE vortex_lock CASCADE CONSTRAINTS;

CREATE TABLE vortex_lock
(
    lock_id NUMBER NOT NULL,
    resource_id NUMBER NOT NULL,
    token VARCHAR2 (128) NOT NULL,
    lock_type_id NUMBER NOT NULL,
    lock_owner VARCHAR2 (64) NOT NULL,
    lock_owner_info VARCHAR2 (64) NOT NULL,
    depth CHAR (1) DEFAULT '1' NOT NULL,
    timeout DATE NOT NULL
);

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_PK
PRIMARY KEY (lock_id);

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id) ON DELETE CASCADE;
;

-- ALTER TABLE vortex_lock
--     ADD CONSTRAINT vortex_lock_FK_1 FOREIGN KEY (resource_id)
--     REFERENCES vortex_resource (resource_id)
-- ;

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_FK_2 FOREIGN KEY (lock_type_id)
    REFERENCES lock_type (lock_type_id)
;

-----------------------------------------------------------------------------
-- action_type
-----------------------------------------------------------------------------
DROP TABLE action_type CASCADE CONSTRAINTS;

CREATE TABLE action_type
(
    action_type_id NUMBER NOT NULL,
    namespace VARCHAR2 (64) NOT NULL,
    name VARCHAR2 (64) NOT NULL
);

ALTER TABLE action_type
    ADD CONSTRAINT action_type_PK
PRIMARY KEY (action_type_id);


-----------------------------------------------------------------------------
-- acl_entry
-----------------------------------------------------------------------------
DROP SEQUENCE acl_entry_seq_pk;

CREATE SEQUENCE acl_entry_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE acl_entry CASCADE CONSTRAINTS;

CREATE TABLE acl_entry
(
    acl_entry_id NUMBER NOT NULL,
    resource_id NUMBER NOT NULL,
    action_type_id NUMBER NOT NULL,
    user_or_group_name VARCHAR2 (64) NOT NULL,
    is_user CHAR (1) DEFAULT 'Y' NOT NULL,
    granted_by_user_name VARCHAR2 (64) NOT NULL,
    granted_date DATE NOT NULL
);

ALTER TABLE acl_entry
    ADD CONSTRAINT acl_entry_PK
PRIMARY KEY (acl_entry_id);

ALTER TABLE acl_entry
    ADD CONSTRAINT acl_entry_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id) ON DELETE CASCADE
;

-- ALTER TABLE acl_entry
--     ADD CONSTRAINT acl_entry_FK_1 FOREIGN KEY (resource_id)
--     REFERENCES vortex_resource (resource_id)
-- ;

ALTER TABLE acl_entry
    ADD CONSTRAINT acl_entry_FK_2 FOREIGN KEY (action_type_id)
    REFERENCES action_type (action_type_id)
;


-----------------------------------------------------------------------------
-- extra_prop_entry
-----------------------------------------------------------------------------
DROP SEQUENCE extra_prop_entry_seq_pk;

CREATE SEQUENCE extra_prop_entry_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE extra_prop_entry CASCADE CONSTRAINTS;

CREATE TABLE extra_prop_entry
(
    extra_prop_entry_id NUMBER NOT NULL,
    resource_id NUMBER NOT NULL,
    name_space VARCHAR2 (128) NOT NULL,
    name VARCHAR2 (64) NOT NULL,
    value VARCHAR2 (2048) NOT NULL
);

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_PK
PRIMARY KEY (extra_prop_entry_id);

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id) ON DELETE CASCADE
;

-- ALTER TABLE extra_prop_entry
--     ADD CONSTRAINT extra_prop_entry_FK_1 FOREIGN KEY (resource_id)
--     REFERENCES vortex_resource (resource_id)
-- ;



-----------------------------------------------------------------------------
-- changelog_entry
-----------------------------------------------------------------------------
DROP SEQUENCE changelog_entry_seq_pk;

CREATE SEQUENCE changelog_entry_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE changelog_entry CASCADE CONSTRAINTS;

/* The attribute 'uri' can't be longer that 1578 chars (OS-dependent?).     */
/* If bigger -> "ORA-01450: maximum key length exceeded" (caused by index). */
/* Since combined index '(uri, changelog_entry_id)' -> 1500 chars.          */

CREATE TABLE changelog_entry
(
    changelog_entry_id NUMBER NOT NULL,
    logger_id NUMBER NOT NULL,
    logger_type NUMBER NOT NULL,
    operation VARCHAR2 (128) NULL,
    timestamp DATE NOT NULL,
    uri NVARCHAR2 (1500) NOT NULL,
    resource_id NUMBER,
    is_collection CHAR(1) DEFAULT 'N' NOT NULL
);

ALTER TABLE changelog_entry
    ADD CONSTRAINT changelog_entry_PK
PRIMARY KEY (changelog_entry_id);

DROP INDEX changelog_entry_index1;

CREATE UNIQUE INDEX changelog_entry_index1
    ON changelog_entry (uri, changelog_entry_id);




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
    vortex_resource_seq_pk.nextval,
    '/',
    sysdate,
    sysdate,
    sysdate,
    'vortex',
    'vortex',
    'vortex',
    '/',
    'unknown',
    'application/x-vortex-collection',
    null,
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
    acl_entry_seq_pk.nextval,
    vortex_resource_seq_pk.currval,
    1,
    'dav:authenticated',
    'Y',
    'vortex',
    sysdate
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
    acl_entry_seq_pk.nextval,
    vortex_resource_seq_pk.currval,
    4,
    'dav:all',
    'Y',
    'vortex',
    sysdate
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
    acl_entry_seq_pk.nextval,
    vortex_resource_seq_pk.currval,
    1,
    'dav:owner',
    'Y',
    'vortex',
    sysdate
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
    acl_entry_seq_pk.nextval,
    vortex_resource_seq_pk.currval,
    2,
    'dav:owner',
    'Y',
    'vortex',
    sysdate
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
    acl_entry_seq_pk.nextval,
    vortex_resource_seq_pk.currval,
    3,
    'dav:owner',
    'Y',
    'vortex',
    sysdate
);    
