
-- add table definition for comments

create sequence vortex_comment_seq_pk increment 1 start 1000;

create table vortex_comment
(
    id int not null,
    resource_id int not null,
    author varchar(64) not null,
    time timestamp not null,
    title varchar(2048) not null,
    text varchar(4096) not null,
    approved char(1) default 'Y' not null,
    constraint vortex_comment_id_constraint unique (id)
);

alter table vortex_comment
      add constraint vortex_comment_pk primary key (id);

alter table vortex_comment
      add constraint vortex_comment_fk foreign key (resource_id)
          references vortex_resource (resource_id);


-- add 'add-comment' privilege

insert into action_type (action_type_id, name) values (6, 'add-comment');
