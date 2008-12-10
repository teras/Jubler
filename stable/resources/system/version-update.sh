#!/bin/bash

FPATH=`dirname $0`/version.prop

RELEASE=`svn info | grep Revision | awk '{print $2'}`

echo -n "Please give human-readable version: "
read HVER
echo -n "Please give numeric version: "
read NVER
echo
echo "Human readable version: $HVER"
echo "Numeric version: $NVER"
echo "Release: $RELEASE"
echo "Press [RETURN] to continue or [CTRL]-C to abort version upgrade."
read NL
echo
echo Updating file $FPATH
cat >$FPATH <<EOF
version=$HVER
longversion=$NVER
release=${RELEASE}
packaged=@DISTRIBUTION@
EOF
cd `dirname $0`
cd ../..

ant nodistbased
echo
echo "Version update completed"
