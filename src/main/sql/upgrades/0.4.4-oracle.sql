
-- Extend the length of the 'content_type' column:

alter table vortex_resource modify 
(
    content_type varchar2(128)
);

