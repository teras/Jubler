#!/bin/bash

BUNDLEID=com.panayotis.jubler
PASSLOC=../../resources/keys/notarizing-key.sh
DMGLOC=target
USERNAME=panayotis@panayotis.com
SLEEP=10

cd `dirname $0`

source "$PASSLOC"

if [ -z "$APPLE_APP_PASSWORD" ] ; then echo Unable to locate Apple app password ; exit 1 ; fi

DMG=`ls "$DMGLOC" 2>/dev/null |grep dmg\$`
if [ -z "$DMG" ] ; then echo "No DMG found" ; exit 1 ; fi


echo Send DMG $DMG to Apple
xcrun &>target/send.log altool -t osx -f "$DMGLOC"/$DMG  --primary-bundle-id "$BUNDLEID"  --notarize-app --username "$USERNAME" --password "$APPLE_APP_PASSWORD"
cat target/send.log

NOERROR=`grep <target/send.log "No errors"`
if [ -z "$NOERROR" ] ; then echo ; echo Error while uploading ; exit 1 ; fi

UUID=`grep <target/send.log RequestUUID | awk -F '=' '{print $2;}'`
echo "# xcrun altool --notarization-info $UUID -u panayotis@panayotis.com -p [PASSWORD]"

while true ; do
    echo Sleeping for $SLEEP'"'
    sleep $SLEEP
    echo Check status of package
    xcrun &>target/check.log altool --notarization-info $UUID -u panayotis@panayotis.com -p $APPLE_APP_PASSWORD
    cat target/check.log

    APPROVED=`grep <target/check.log "Package Approved"`
    if [ -n "$APPROVED" ] ; then
        echo Stapling DMG
        xcrun stapler staple -v "$DMGLOC"/$DMG
        exit 0
    fi

    INPROGRESS=`grep <target/check.log "in progress"`
    if [ -z "$INPROGRESS" ] ; then echo Error found, exiting ; exit 1 ; fi
done

