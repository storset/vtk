-- Create missing indices on foreign keys for content revisions:
create index simple_content_revision_index1 on simple_content_revision(resource_id);

create index revision_acl_entry_index1 on revision_acl_entry(revision_id);
