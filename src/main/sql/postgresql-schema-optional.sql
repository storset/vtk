-----------------------------------------------------------------------------
-- VORTEX DDL, extra contrib functions for PostgreSQL
-----------------------------------------------------------------------------


-- Automatic clean up of binary large objects through trigger and 'lo' extension.
-- Requires:
--     - PostgreSQL contrib modules to be installed on system where database runs.
--     - Database super-user access.

-- 0. Install PostgreSQL contrib modules, if it's not present in the database installation.

-- 1. Connect to Vortex database as super-user and run all SQL statements in the
--    PostgreSQL lo contrib file: 'contrib/lo/lo.sql' (location depends on installation).

-- 2. Run the following to set up trigger for extra_prop_entry.binary_content:

alter table extra_prop_entry alter column binary_content TYPE lo;

create trigger extra_prop_entry_trigger1 
   before update or delete on extra_prop_entry for each row execute procedure lo_manage(binary_content);
