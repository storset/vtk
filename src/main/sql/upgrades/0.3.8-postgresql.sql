-- Rename existing 'title' to 'userTitle':

update extra_prop_entry set name = 'userTitle' where name = 'title' and name_space is null;


-- Set 'title' to last part of URI for everything except root ('/')

insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk'),
       resource_id,
       0,
       null,
       'title',
       substring(uri from '[^/]+$')
from vortex_resource where uri != '/';


-- Set 'title' for root ('/') too

insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk'),
       resource_id,
       0,
       null,
       'title',
       '/'
from vortex_resource where uri = '/';



-- Temporarily remove 'title' for only those resources now having
-- 'userTitle':

delete from extra_prop_entry
   where name = 'title' and name_space
         is null and resource_id in
         (select resource_id from extra_prop_entry
             where name = 'userTitle' and name_space is null);

-- Insert 'title' (= 'usertitle') for those resources:

insert into extra_prop_entry 
select nextval('extra_prop_entry_seq_pk',
       resource_id,
       0,
       null,
       'title',
       value
from extra_prop_entry
where name = 'userTitle' and name_space is null;

commit;
