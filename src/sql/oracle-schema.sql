-----------------------------------------------------------------------------
-- VORTEX DDL $Id: oracle-schema.sql,v 1.12 2004/03/19 17:08:47 gormap Exp $
-- This file contains definitions of the tables required by the
-- OracleDatabase class. 
-----------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- resource
-- TODO: allow null values for content_language
-----------------------------------------------------------------------------

DROP SEQUENCE vortex_resource_seq_pk;

CREATE SEQUENCE vortex_resource_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE vortex_resource CASCADE CONSTRAINTS;

CREATE TABLE vortex_resource
(
    resource_id NUMBER NOT NULL,
    prev_resource_id NUMBER NULL, -- used when copying/moving
    uri VARCHAR2 (2048) NOT NULL,
    depth NUMBER NOT NULL,
    creation_time TIMESTAMP NOT NULL,
    created_by VARCHAR2 (64) NOT NULL,
    content_last_modified TIMESTAMP NOT NULL,
    properties_last_modified TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    content_modified_by VARCHAR2 (64) NOT NULL,
    properties_modified_by VARCHAR2 (64) NOT NULL,
    modified_by VARCHAR2 (64) NOT NULL,
    resource_owner VARCHAR2 (64) NOT NULL,
    display_name VARCHAR2 (128) NULL,
    content_language VARCHAR2 (64) NULL,
    content_type VARCHAR2 (64) NULL,
    content_length NUMBER NULL,
    resource_type VARCHAR2 (64) NOT NULL,
    character_encoding VARCHAR2 (64) NULL,
    guessed_character_encoding VARCHAR2 (64) NULL,
    user_character_encoding VARCHAR2 (64) NULL,
    is_collection CHAR(1) DEFAULT 'N' NOT NULL,
    acl_inherited_from NUMBER NULL,
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
-- resource_ancestor_ids
-- Stored function for retrieving a resource's ancestor IDs.
-- Returns a VARCHAR of space-separated id integers.
-----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION resource_ancestor_ids(uri IN VARCHAR)
RETURN VARCHAR AS
  ancestor VARCHAR(1500) DEFAULT '/';
  id NUMBER;
  ids VARCHAR(1500) DEFAULT '';
  slashpos NUMBER DEFAULT 1;
BEGIN
  IF uri = '/' THEN RETURN ids; END IF;

  LOOP
    SELECT vr.resource_id INTO id FROM vortex_resource vr
    WHERE vr.uri = ancestor AND vr.is_collection = 'Y';
    ids := ids || ' ' || id;
    slashpos := instr(uri, '/', slashpos+1);
    EXIT WHEN slashpos = 0;
    ancestor := substr(uri, 1, slashpos-1);
  END LOOP;
  RETURN ids;
END resource_ancestor_ids;
/
show errors;


-----------------------------------------------------------------------------
-- vortex_tmp - Auxiliary temp-table used to hold lists of URIs or resource-
--              IDs
-----------------------------------------------------------------------------
DROP SEQUENCE vortex_tmp_session_id_seq;
CREATE SEQUENCE vortex_tmp_session_id_seq INCREMENT BY 1 START WITH 1000;

DROP TABLE vortex_tmp;
CREATE TABLE vortex_tmp (
    session_id NUMBER,
    resource_id NUMBER,
    uri VARCHAR2(2048)
);

CREATE INDEX vortex_tmp_index ON vortex_tmp(uri);




-----------------------------------------------------------------------------
-- vortex_lock
-----------------------------------------------------------------------------
DROP SEQUENCE vortex_lock_seq_pk;

CREATE SEQUENCE vortex_lock_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE vortex_lock CASCADE CONSTRAINTS;

CREATE TABLE vortex_lock
(
    lock_id NUMBER NOT NULL,
    resource_id NUMBER NOT NULL,
    token VARCHAR2 (128) NOT NULL,
    lock_owner VARCHAR2 (128) NOT NULL,
    lock_owner_info VARCHAR2 (128) NOT NULL,
    depth CHAR (1) DEFAULT '1' NOT NULL,
    timeout TIMESTAMP NOT NULL
);

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_PK
PRIMARY KEY (lock_id);

ALTER TABLE vortex_lock
    ADD CONSTRAINT vortex_lock_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id) ON DELETE CASCADE;
;

CREATE INDEX vortex_lock_index1 ON vortex_lock(resource_id);
CREATE INDEX vortex_lock_index2 ON vortex_lock(timeout);

-----------------------------------------------------------------------------
-- action_type
-----------------------------------------------------------------------------
DROP TABLE action_type CASCADE CONSTRAINTS;

CREATE TABLE action_type
(
    action_type_id NUMBER NOT NULL,
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

ALTER TABLE acl_entry
    ADD CONSTRAINT acl_entry_FK_2 FOREIGN KEY (action_type_id)
    REFERENCES action_type (action_type_id)
;

CREATE INDEX acl_entry_index1 ON acl_entry(resource_id);


-----------------------------------------------------------------------------
-- prop_type
-----------------------------------------------------------------------------
DROP TABLE prop_type CASCADE CONSTRAINTS;

CREATE TABLE prop_type
(
    prop_type_id NUMBER NOT NULL,
    prop_type_name VARCHAR2 (64) NOT NULL
);

ALTER TABLE prop_type
    ADD CONSTRAINT prop_type_PK PRIMARY KEY (prop_type_id);


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
    prop_type_id NUMBER DEFAULT 0 NOT NULL,
    name_space VARCHAR2 (128) NULL,
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

ALTER TABLE extra_prop_entry
    ADD CONSTRAINT extra_prop_entry_FK_2 FOREIGN KEY (prop_type_id)
    REFERENCES prop_type(prop_type_id) ON DELETE CASCADE
;


CREATE INDEX extra_prop_entry_index1 ON extra_prop_entry(resource_id);

-----------------------------------------------------------------------------
-- changelog_entry
-----------------------------------------------------------------------------
DROP SEQUENCE changelog_entry_seq_pk;

CREATE SEQUENCE changelog_entry_seq_pk INCREMENT BY 1 START WITH 1000;

DROP TABLE changelog_entry CASCADE CONSTRAINTS;

CREATE TABLE changelog_entry
(
    changelog_entry_id NUMBER NOT NULL,
    logger_id NUMBER NOT NULL,
    logger_type NUMBER NOT NULL,
    operation VARCHAR2 (128) NULL,
    timestamp DATE NOT NULL,
    uri VARCHAR2 (2048) NOT NULL,
    resource_id NUMBER,
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

INSERT INTO action_type (action_type_id, name) VALUES (1, 'read');
INSERT INTO action_type (action_type_id, name) VALUES (2, 'write');
INSERT INTO action_type (action_type_id, name) VALUES (3, 'all');
INSERT INTO action_type (action_type_id, name) VALUES (4, 'read-processed');
INSERT INTO action_type (action_type_id, name) VALUES (5, 'bind');

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
    display_name,
    content_language,
    content_type,
    character_encoding,
    is_collection,
    acl_inherited_from,
    content_length,
    resource_type)
VALUES (
    vortex_resource_seq_pk.nextval,
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
    '/',
    NULL,
    'application/x-vortex-collection',
    NULL,
    'Y',
    NULL,
    NULL,
    'collection'
);


-- (pseudo:authenticated (read))

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
    'pseudo:authenticated',
    'Y',
    'vortex@localhost',
    sysdate
);    


-- (pseudo:all, read-processed)

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
    'pseudo:all',
    'Y',
    'vortex@localhost',
    sysdate
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
    acl_entry_seq_pk.nextval,
    vortex_resource_seq_pk.currval,
    3,
    'pseudo:owner',
    'Y',
    'vortex@localhost',
    sysdate
);    


-- Property value types
-- This data currently corresponds to definitions in 
-- org.vortikal.repository.resourcetype.PropertyType
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (0, 'String');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (1, 'Integer');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (2, 'Long');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (3, 'Date');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (4, 'Boolean');
INSERT INTO prop_type (prop_type_id, prop_type_name) VALUES (5, 'Principal');
