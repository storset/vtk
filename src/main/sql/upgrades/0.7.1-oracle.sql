
delete from extra_prop_entry
  where name_space = 'http://www.uio.no/__vtk/ns/structured-resources' and name = 'employees';

delete from extra_prop_entry
  where name_space = 'http://www.uio.no/scientific' and name = 'disciplines';