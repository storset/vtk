-- Set ordering requirement and caching on changelog_entry_seq_pk sequence

alter sequence changelog_entry_seq_pk cache 40 order;
commit;
exit;

