#!/bin/bash
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

# Collects jubler.exe and everything it loads into one folder, for the
# installer. Runs in an MSYS2 UCRT64 shell: deploy.sh <build dir> <target dir>
set -euo pipefail

build=$1
out=$2
prefix=${MINGW_PREFIX:-/ucrt64}

rm -rf "$out"
mkdir -p "$out"
cp "$build/jubler.exe" "$out/"

# Qt: its libraries, the plugins the modules need (platform, styles, image
# formats, the SVG icon engine, TLS) and Qt's own translations.
windeployqt6 --release --no-compiler-runtime --no-system-d3d-compiler --no-opengl-sw "$out/jubler.exe"

# Everything else: libmpv and FFmpeg, hunspell, OpenSSL, and the libraries all
# of them load in turn (windeployqt does not follow non-Qt dependencies). ntldd -R
# follows the whole tree, so the executable and the plugins are read once, and
# then only what that added. System DLLs stay out.
dependencies() {   # the /ucrt64/bin libraries the given files load, recursively
    ntldd -R "$@" 2>/dev/null | awk '{print $3}' | tr '\\' '/' \
        | while IFS= read -r p; do [ -n "$p" ] && cygpath -u "$p"; done \
        | grep -i "^$prefix/bin/" | sort -u
}
mapfile -t scan < <(find "$out" -type f \( -iname '*.exe' -o -path "$out/*/*.dll" \))
while [ "${#scan[@]}" -gt 0 ]; do
    added=()
    while IFS= read -r dll; do
        name=$(basename "$dll")
        if [ ! -e "$out/$name" ]; then
            cp "$dll" "$out/"
            added+=("$out/$name")
        fi
    done < <(dependencies "${scan[@]}")
    scan=("${added[@]}")
done

echo "Deployed $(find "$out" -type f | wc -l) files into $out"
