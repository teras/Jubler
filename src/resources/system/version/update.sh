#!/bin/sh


BASE=`dirname $0`/../../../
FROM=resources/system/version/version.prop
TO=src/com/panayotis/jubler/information/version.prop

RELEASEID=`svn info | grep Revision | gawk '{print $2}'`
sed <$FROM >$TO -e "s/RELEASEID/$RELEASEID/g"
