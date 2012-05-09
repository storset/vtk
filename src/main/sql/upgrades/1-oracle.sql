alter table extra_prop_entry add(is_inheritable char(1) default 'N' not null);
create index extra_prop_entry_index2 on extra_prop_entry(is_inheritable);
