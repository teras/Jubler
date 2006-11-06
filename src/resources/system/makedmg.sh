#!/bin/sh

# create template dmg image
# hdiutil create -size 10m -fs HFS+ -volname Jubler jubler-template.dmg
#

MP=/Volumes/Jubler-template
TEMPL=Jubler-template.dmg

VER=$1

echo Creating DMG for Jubler version $VER
rm -f /tmp/$TEMPL /tmp/$TEMPL.bz2
cp ./$TEMPL.bz2 /tmp
bunzip2 /tmp/$TEMPL.bz2
hdiutil attach -noautoopen -mountpoint $MP /tmp/$TEMPL

echo Copying files
rm -rf $MP/Jubler.app
cp -r ../../dist/Jubler.app $MP
mkdir -p $MP/Jubler.app/Contents/Resources/Java/lib
cp ../ffdecode/libffdecode.jnilib $MP/Jubler.app/Contents/Resources/Java/lib
cp freesans.ttf $MP/Jubler.app/Contents/Resources/Java/lib
hdiutil detach -force $MP

echo Creating final image
hdiutil convert /tmp/$TEMPL -format UDZO -imagekey zlib-level=9 -ov -o ../../Jubler-$VER.dmg
rm -rf /tmp/$TEMPL
