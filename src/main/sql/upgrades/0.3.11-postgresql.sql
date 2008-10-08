DROP SEQUENCE binary_prop_entry_seq_pk;

CREATE SEQUENCE binary_prop_entry_seq_pk INCREMENT 1 START 1000;

DROP TABLE binary_prop_entry;

CREATE TABLE binary_prop_entry
(
  binary_prop_entry_id int NOT NULL,
  resource_id int NOT NULL,
  binary_prop_ref varchar(2048) NOT null,
  binary_mimetype VARCHAR (32) NOT NULL,
  binary_content oid NOT NULL
);

ALTER TABLE binary_prop_entry ADD CONSTRAINT binary_prop_entry_PK PRIMARY KEY (binary_prop_entry_id);

ALTER TABLE binary_prop_entry ADD CONSTRAINT binary_prop_entry_FK_1 FOREIGN KEY (resource_id)
    REFERENCES vortex_resource (resource_id) ON DELETE CASCADE;