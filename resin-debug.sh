#!/bin/sh

$RESIN_HOME/bin/httpd.sh -conf target/resin.conf -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=12345
