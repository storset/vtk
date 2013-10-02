-- Delete property 'obsoleted'. Will be replaced by contextual property "unpublishedCollection" 

delete from extra_prop_entry where name = 'obsoleted';

commit;
exit;