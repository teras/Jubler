#!/bin/bash

FPATH=`dirname $0`/version.prop

echo -n "Please give human-readable version: "
read HVER
echo -n "Please give numeric version: "
read NVER
echo
echo "Human readable version is \"$HVER\" and numeric version is \"$NVER\"."
echo "Press [RETURN] to continue or [CTRL]-C to abort version upgrade."
read NL
cat >$FPATH <<EOF
version=$HVER
longversion=$NVER
release=@RELEASE@
packaged=@DISTRIBUTION@
EOF
cd `dirname $0`
cd ../..
ant version-update
echo
echo "Version update completed"
