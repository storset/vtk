
-- Add column is_inheritable on table extra_prop_entry:
alter table extra_prop_entry add(is_inheritable char(1) default 'N' not null);

-- Add index for new column is_inheritable on table extra_prop_entry:
create index extra_prop_entry_index2 on extra_prop_entry(is_inheritable);

-- Convert column vortex_resoruce.content_language to inheritable property
-- stored in extra_prop_entry:
insert into extra_prop_entry
  select nextval('extra_prop_entry_seq_pk'), resource_id, 0, null, 'contentLocale', content_language, null, null, 'Y' from vortex_resource
  where content_language is not null;

-- Drop now obsolete column vortex_resource.content_language:
-- Not necessary to do right away, can be done later, when functionality is verified correct:
-- alter table vortex_resource drop column content_language;

-- Convert property 'commentsEnabled' to be inheritable:
update extra_prop_entry set is_inheritable = 'Y' where name_space is null and name = 'commentsEnabled';
