-- 1. Support for binary properties (extend extra_prop_entry definition):

ALTER TABLE extra_prop_entry ADD COLUMN binary_mimetype varchar (64);=======
ALTER TABLE extra_prop_entry ADD COLUMN binary_mimetype varchar (64);

-- 2. This version introduces a new image caption property in the
-- "introduction" mixin type. Earlier, image captions were fetched
-- from the 'description' property on the referred images and
-- displayed on the article/event/folder/whatever. This SQL gets image
-- captions from the description of referred images and inserts it as
-- caption properties into the "extra_prop_entry" table.


-- temporary tables for storing image captions

create table tmp_img_caption_ref
(
        uri varchar(2048),            -- uri of the article
        resource_id int,              -- resource ID of the article
        caption_uri varchar(2048)     -- uri of the image
);

create table tmp_img_caption 
(
       resource_id int,              -- resource ID of the article
       caption varchar(2048)         -- the caption text
);


-- insert caption data into temporary tables:

insert into tmp_img_caption_ref
select r.uri, r.resource_id, p.value 
       from extra_prop_entry p, vortex_resource r
       where p.name = 'picture' and p.name_space is null and r.resource_id = p.resource_id
       and p.value in
          (select uri from vortex_resource
               where resource_type = 'image' and resource_id in (
                  select resource_id from extra_prop_entry
	     	       where name = 'description' and name_space = 'http://www.uio.no/content'));

insert into tmp_img_caption
select tmp.resource_id,
       description.value
from tmp_img_caption_ref tmp, vortex_resource r2, extra_prop_entry description
       where r2.uri = tmp.caption_uri
       and description.resource_id = r2.resource_id
       and description.name =  'description'
       and description.name_space = 'http://www.uio.no/content';


-- insert new caption properties into extra_prop_entry:

-- case 1: where userTitle has been set on the resource:

insert into extra_prop_entry
select nextval('extra_prop_entry_seq_pk'),
       tmp.resource_id,
       0,
       null,
       'caption',
       '<p><b>' || p.value || ':</b> ' || tmp.caption || '</p>',
       null,
       null
from tmp_img_caption tmp, extra_prop_entry p
       where tmp.resource_id = p.resource_id and p.name = 'userTitle' and p.name_space is null;

-- case 1: where no userTitle exists:

insert into extra_prop_entry
select nextval('extra_prop_entry_seq_pk'),
       tmp.resource_id,
       0,
       null,
       'caption',
       '<p>' || tmp.caption || '</p>',
       null,
       null
from tmp_img_caption tmp
       where tmp.resource_id not in (select resource_id from extra_prop_entry where name = 'userTitle' and name_space is null);


-- remove temporary tables:

drop table tmp_img_caption_ref;
drop table tmp_img_caption;

commit;
>>>>>>> .merge-right.r4681
