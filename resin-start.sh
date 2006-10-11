#!/bin/sh

CONF=target/vortikal-0.3.2-SNAPSHOT/WEB-INF/classes/resin.conf

$RESIN_HOME/bin/httpd.sh -conf $CONF
