#!/bin/bash

set -e

cd "`dirname \"$0\"`"

mkdir upload

mvn clean install -Pdist-osx,notarize
cp modules/installer/target/Jubler-*.dmg upload/

mvn clean install -Pdist-generic
cp modules/installer/target/Jubler-*tar.bz2 upload/

mvn clean install -Pdist-linux
cp modules/installer/target/Jubler-*.appimage upload/

mvn clean install -Pdist-windows
cp modules/installer/target/Jubler-*.exe upload/


