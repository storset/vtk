
-- Rename resource type 'portal-page' -> 'collection': 
update vortex_resource set resource_type = 'collection' where resource_type = 'portal-page';
