
-- Rename property 'skipInPath' to 'navigation:hidden' in extra_prop_entry

UPDATE extra_prop_entry 
   SET name = 'hidden', 
       name_space = 'http://www.uio.no/navigation' 
WHERE name = 'skipInPath' AND name_space IS NULL;
