#!/bin/sh

RELEASEID=`svn info | grep Revision | gawk '{print $2}'`

BASE=`dirname $0`
FROM=$BASE/version.prop
TO=$BASE/../../../src/com/panayotis/jubler/information/version.prop
sed <$FROM >$TO -e "s/RELEASEID/$RELEASEID/g"
