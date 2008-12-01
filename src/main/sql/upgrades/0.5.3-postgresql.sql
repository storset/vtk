-- 1. Add the 'numberOfComments' property for resources that have been commentd

create table tmp_number_of_comments
(
  resource_id int,
  number_of_comments int
);

insert into tmp_number_of_comments
  select distinct(resource_id), count(*) from vortex_comment group by resource_id;

insert into extra_prop_entry
  select nextval('extra_prop_entry_seq_pk'),
    tmp.resource_id,
    0,
    null,
    'numberOfComments',
    tmp.number_of_comments,
    null,
    null
  from tmp_number_of_comments tmp, vortex_resource r
    where tmp.resource_id = r.resource_id;

drop table tmp_number_of_comments;

insert into extra_prop_entry (extra_prop_entry_id, resource_id, value, name)
  values (nextval('extra_prop_entry_seq_pk'), 
  (select t.resource_id, t.number_of_comments
    from tmp_number_of_comments t, vortex_resource r
      where t.resource_id = r.resource_id),
  'numberOfComments');

drop table tmp_number_of_comments;


-- 2. This version splits the keywords property. Articles and events now
-- have a tags property in the "introduction" mixin type. This SQL renames
-- keywords properties to tags properties on these resource types

update extra_prop_entry
    set name = 'tags', name_space = NULL where name = 'keywords' AND
    resource_id in (select resource_id from vortex_resource where 
        resource_type in ('article', 'event'));

commit;
