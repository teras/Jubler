#!/bin/bash

set -e

cd "`dirname \"$0\"`"

mkdir -p dist

mvn clean install -Pdist,generic
cp modules/installer/target/Jubler-*tar.bz2 dist/

mvn clean install -Pdist,linux
cp modules/installer/target/Jubler-*.appimage dist/

exit 0


mvn clean install -Pdist,windows
cp modules/installer/target/Jubler-*.exe dist/

mvn clean install -Pdist,macos,notarize
cp modules/installer/target/Jubler-*.dmg dist/
