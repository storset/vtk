set heading off
set count off
spool grant_to_vrtx.sql
select 'grant delete, select, insert, update on ' || table_name || ' to vrtx;' from user_tables;
spool off
