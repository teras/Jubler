#!/bin/bash

set -e
cd ..
notarizing sign  -x JupidatorUpdate -e ../../../../resources/installers/macosx/entitlements.plist
