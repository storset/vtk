#!/bin/sh

CONF=`ls -d target/vortikal-*/`resin.conf

# check if RESIN_HOME is set
if test -z $RESIN_HOME; then
    echo "\$RESIN_HOME must be set to use this script";
    echo "(set it in .bashrc)";
else
    $RESIN_HOME/bin/httpd.sh -conf $CONF "$@";
fi
