-- Replace 'vortex_resource_depth_index' with a combined index on both depth and URI:

drop index vortex_resource_depth_index;

create index vortex_resource_depth_uri_index on vortex_resource(depth, uri);


