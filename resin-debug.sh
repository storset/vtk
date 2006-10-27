#!/bin/sh

CONF=`ls -d target/vortikal-*/`resin.conf

DEBUG_PARAMS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=12345"
if test -z $RESIN_HOME; then
    echo "\$RESIN_HOME must be set to use this script";
    echo "(set it in .bashrc)";
else
    $RESIN_HOME/bin/httpd.sh -conf $CONF $DEBUG_PARAMS
fi

