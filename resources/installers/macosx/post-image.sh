#!/bin/bash

set -e
cd ..
notarizing sign  -x JupidatorUpdate -e ../../../../resources/installers/macosx/entitlements.plist
cd images
cd $(ls)
rm Jubler.app/Contents/runtime/Contents/MacOS/libjli.dylib
