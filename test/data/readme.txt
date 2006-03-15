Add the following to build.properties to use the testdatabase. Remember to update the path to the testdata.


repositoryDataDirectory = /Users/kajh/src/tavle2/vortikal/trunk/test/data/vortex-documents

jdbcUsername = sa
jdbcPassword =
databaseURL = jdbc:hsqldb:file:/Users/kajh/src/tavle2/vortikal/trunk/test/data/hsqldb/testdb
databaseDriver = org.hsqldb.jdbcDriver


