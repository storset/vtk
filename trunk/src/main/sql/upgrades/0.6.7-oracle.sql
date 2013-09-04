/* Re-insert publish-date property for all 'legacy' resources that have lost it since release 0.6 */

insert into extra_prop_entry 
select extra_prop_entry_seq_pk.nextval,
       resource_id,
       0,
       null,
       'publish-date',
       to_char(creation_time, 'YYYY-MM-DD HH24:MI:SS'),
       null,
       null
from vortex_resource 
where resource_id in
    (select resource_id from extra_prop_entry
       where name_space is null and name = 'published' and value = 'true')
and resource_id not in 
    (select resource_id from extra_prop_entry
          where name_space is null and name = 'publish-date');

commit;
exit;
