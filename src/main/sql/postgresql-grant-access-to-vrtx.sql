/* 
PRECONDITIONS:

* If user is not created, run (in psql as database administrator)

dbname=> CREATE USER <jdbcUsername> WITH PASSWORD '<jdbcPassword>';

where jdbcUsername and jdbcUsername are equal to the properties spesified in the build.properties file.

* Create a database using the DDL in the 'src/sql/postgresql-schema.sql'. Example; 

dbname=> \i /Users/evenh/vortikal/trunk/src/sql/postgresql-schema.sql

* Change 'vrtx' to <jdbcUsername> in the following grant statement if they differ.
*/

GRANT DELETE, SELECT, INSERT, UPDATE ON acl_entry, acl_entry_seq_pk, action_type, changelog_entry, changelog_entry_seq_pk, extra_prop_entry, extra_prop_entry_seq_pk, prop_type, vortex_lock, vortex_lock_seq_pk, vortex_resource, vortex_resource_seq_pk, vortex_tmp_session_id_seq, vortex_tmp TO vrtx;

/*
dbname=> \z (show access privileges for database "dbname")
*/
