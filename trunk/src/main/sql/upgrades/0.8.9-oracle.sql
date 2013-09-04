-- Delete property 'system-job-status':
delete from extra_prop_entry where name_space is null and name = 'system-job-status';

-- Delete invalid value for property
delete from extra_prop_entry where name_space is null and name = 'recursive-listing' and value = 'true';

-- Delete invalid binary properties (no content)
delete from extra_prop_entry where value = '#binary' and binary_content is null;

delete from extra_prop_entry where name = 'thumbnail' and name_space is null and value != '#binary' and binary_content is null;

update extra_prop_entry set value = '#binary' where name = 'thumbnail' and name_space is null and value != '#binary' and binary_content is not null;
