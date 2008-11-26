-- 1. This version splits the keywords property. Articles and events now
-- have a tags property in the "introduction" mixin type. This SQL renames
-- keywords properties to tags properties on these resource types

update extra_prop_entry
    set name = 'tags', name_space = NULL where name = 'keywords' AND
    resource_id in (select resource_id from vortex_resource where 
        resource_type in ('article', 'event'));
commit;
