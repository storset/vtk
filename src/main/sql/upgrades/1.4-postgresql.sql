
-- Delete obsolete property from database. Content is cached at application level.

delete from extra_prop_entry
  where name_space = 'http://www.uio.no/__vtk/ns/structured-resources' and name = 'externalScientificInformation';