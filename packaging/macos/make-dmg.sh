#!/bin/bash
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

# The macOS disk image of the build machine's architecture: Apple Silicon for
# macOS 13 and newer (every such Mac runs 13) with Qt 6.11, Intel for macOS 12
# and newer (the Intel Macs that stop at 12) with Qt 6.9, the last Qt for 12.
# Jubler.app is built with conda-forge's compiler against its Qt (with ICU, which
# the 8-bit charsets need and qt.io's macOS Qt lacks), libmpv, FFmpeg, hunspell
# and OpenSSL, everything it loads copied into the bundle, signed with the
# Developer ID, put into the Java release's styled drag-to-Applications image,
# notarized and stapled.
#   make-dmg.sh <version> <arch suffix: "-arm64" or "-x86_64">
# Signing needs MACOS_CERTIFICATE (base64 .p12), MACOS_CERTIFICATE_PWD and
# APPLE_NOTARY_JSON ({issuer_id, key_id, private_key}); there is no unsigned result.
set -euo pipefail

VERSION=$1
SUFFIX=$2
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
WORK=$ROOT/build-macos
APP=$WORK/Jubler.app
DMG=$ROOT/dist/Jubler-$VERSION$SUFFIX.dmg
DEPS=$WORK/deps
case $(uname -m) in
    arm64) CONDA_PLATFORM=osx-arm64 QT_VERSION=6.11 MACOS_MIN=13.0 ;;
    *)     CONDA_PLATFORM=osx-64    QT_VERSION=6.9  MACOS_MIN=12.0 ;;
esac

for v in MACOS_CERTIFICATE MACOS_CERTIFICATE_PWD APPLE_NOTARY_JSON; do
    [ -n "${!v:-}" ] || { echo "error: $v is not set; a release is never left unsigned" >&2; exit 1; }
done

# --- Libraries that run on that macOS ------------------------------------------
# The solver takes only packages that declare they run on this macOS; Qt's
# version is given all the same, since conda-forge declares macOS 11 even for
# the Qt versions that need 13 or 14. Their libraries use conda-forge's own
# libc++, so Jubler is compiled with its compiler too: one C++ library in the
# process.
mkdir -p "$DEPS"
curl -fsSL "https://micro.mamba.pm/api/micromamba/$CONDA_PLATFORM/latest" | tar -xj -C "$DEPS" bin/micromamba
export MAMBA_ROOT_PREFIX=$DEPS/mamba
CONDA_OVERRIDE_OSX=$MACOS_MIN "$DEPS/bin/micromamba" create -y -p "$DEPS/env" -c conda-forge --override-channels \
    "qt6-main=$QT_VERSION" mpv ffmpeg hunspell openssl zlib cxx-compiler ninja
LIBS=$DEPS/env

# --- Build --------------------------------------------------------------------
# Only the conda-forge libraries, never what the build machine has in Homebrew.
# pkgconf: the prefix from where each .pc file lies (conda-forge's hunspell.pc
# still names its build folder), and only the libraries asked for, not the
# headers of what they use in turn (glib, Python, ... for mpv).
export PKG_CONFIG_LIBDIR=$LIBS/lib/pkgconfig
"$DEPS/bin/micromamba" run -p "$LIBS" cmake -S "$ROOT" -B "$WORK" -G Ninja -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_OSX_DEPLOYMENT_TARGET=$MACOS_MIN -DCMAKE_PREFIX_PATH="$LIBS" \
      -DPKG_CONFIG_EXECUTABLE="$(command -v pkgconf)" -DPKG_CONFIG_ARGN="--define-prefix;--maximum-traverse-depth=1"
"$DEPS/bin/micromamba" run -p "$LIBS" cmake --build "$WORK"
ctest --test-dir "$WORK" --output-on-failure

# --- Icons, as the Java bundle's: the application's and the documents' ----------
icns() {   # icns <svg> <name>
    local set=$WORK/$2.iconset
    rm -rf "$set" && mkdir -p "$set"
    for s in 16 32 128 256 512; do
        rsvg-convert -w $s -h $s -a "$1" -o "$set/icon_${s}x${s}.png"
        rsvg-convert -w $((s * 2)) -h $((s * 2)) -a "$1" -o "$set/icon_${s}x${s}@2x.png"
    done
    iconutil -c icns "$set" -o "$APP/Contents/Resources/$2.icns"
}
mkdir -p "$APP/Contents/Resources"
icns "$ROOT/resources/logo/logo.svg" Jubler
icns "$ROOT/resources/logo/subfile.svg" JublerDocument

# --- Everything it loads, inside the bundle -----------------------------------
"$LIBS/lib/qt6/bin/macdeployqt" "$APP" -verbose=1
# What macdeployqt leaves outside: the non-Qt libraries.
python3 "$ROOT/packaging/macos/bundle-libs.py" "$APP" "$LIBS/lib"
# Nothing may still point outside the bundle and the system.
leaks=$(find "$APP" -type f \( -perm +111 -o -name '*.dylib' \) -exec otool -L {} \; 2>/dev/null \
        | grep -E '^\s+/' | grep -v -E '^\s+(/System/|/usr/lib/)' | sort -u || true)
if [ -n "$leaks" ]; then
    echo "error: the bundle still loads libraries from outside it:" >&2
    echo "$leaks" >&2
    exit 1
fi
# Nor may anything in it need a newer macOS than the one it promises.
too_new=$(find "$APP" -type f -print0 | while IFS= read -r -d '' f; do
    file -b "$f" | grep -q 'Mach-O' || continue
    otool -arch all -l "$f" | awk -v f="$f" -v min="$MACOS_MIN" '
        /cmd LC_BUILD_VERSION|cmd LC_VERSION_MIN_MACOSX/ { want = 1 }
        want && $1 ~ /^(minos|version)$/ {
            want = 0; split($2, v, "."); split(min, m, ".")
            if (v[1] + 0 > m[1] + 0 || (v[1] + 0 == m[1] + 0 && v[2] + 0 > m[2] + 0)) print $2, f
        }'
done | sort -u)
if [ -n "$too_new" ]; then
    echo "error: these need a macOS newer than $MACOS_MIN:" >&2
    echo "$too_new" >&2
    exit 1
fi
"$APP/Contents/MacOS/Jubler" --list-tools | grep -q 'Available tools'

# --- Signing --------------------------------------------------------------------
KEYCHAIN=$WORK/build.keychain
KEYCHAIN_PWD=$(openssl rand -base64 32)
security create-keychain -p "$KEYCHAIN_PWD" "$KEYCHAIN"
security set-keychain-settings -t 3600 -u "$KEYCHAIN"
security unlock-keychain -p "$KEYCHAIN_PWD" "$KEYCHAIN"
security list-keychains -d user -s "$KEYCHAIN" $(security list-keychains -d user | tr -d '"')
echo "$MACOS_CERTIFICATE" | base64 --decode > "$WORK/certificate.p12"
security import "$WORK/certificate.p12" -k "$KEYCHAIN" -P "$MACOS_CERTIFICATE_PWD" -T /usr/bin/codesign
rm -f "$WORK/certificate.p12"
security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$KEYCHAIN_PWD" "$KEYCHAIN" >/dev/null
IDENTITY=$(security find-identity -v -p codesigning "$KEYCHAIN" | grep "Developer ID Application" | head -1 | awk '{print $2}')
[ -n "$IDENTITY" ] || { echo "error: no Developer ID Application identity in the certificate" >&2; exit 1; }

sign() { codesign --force --timestamp --options runtime --entitlements "$ROOT/packaging/macos/entitlements.plist" --sign "$IDENTITY" "$@"; }
xattr -cr "$APP"
# Inside out: every Mach-O file but the main executable, then the frameworks,
# then the bundle (which signs the main executable with the entitlements).
find "$APP/Contents" -type f ! -path "$APP/Contents/MacOS/*" | while read -r f; do
    if file "$f" | grep -q 'Mach-O'; then sign "$f"; fi
done
find "$APP/Contents/Frameworks" -maxdepth 1 -name '*.framework' -exec codesign --force --timestamp --options runtime --sign "$IDENTITY" {} \;
sign "$APP"
codesign --verify --deep --strict --verbose=2 "$APP"

# --- The disk image, from the Java release's template -----------------------------
TEMPLATE_DIR=$WORK/dmg-template
rm -rf "$TEMPLATE_DIR" && mkdir -p "$TEMPLATE_DIR"
unzip -q "$ROOT/packaging/macos/dmg_template.zip" -d "$TEMPLATE_DIR"
hdiutil attach "$TEMPLATE_DIR/Jubler-template.dmg" -readonly -nobrowse -mountpoint "$TEMPLATE_DIR/mnt"
SIZE_MB=$(( $(du -sm "$APP" | cut -f1) + 60 ))
RW=$WORK/Jubler-rw.dmg
rm -f "$RW"
hdiutil create -size ${SIZE_MB}m -fs HFS+ -volname Jubler "$RW"
hdiutil attach "$RW" -readwrite -nobrowse -mountpoint "$WORK/rw"
# The window layout and background of the template; its application is replaced.
rsync -a --exclude '*.app' --exclude 'Applications' --exclude '.fseventsd' --exclude '.Trashes' "$TEMPLATE_DIR/mnt/" "$WORK/rw/"
ditto "$APP" "$WORK/rw/Jubler.app"
ln -s /Applications "$WORK/rw/Applications"
hdiutil detach "$WORK/rw"
hdiutil detach "$TEMPLATE_DIR/mnt"
mkdir -p "$(dirname "$DMG")"
rm -f "$DMG"
hdiutil convert "$RW" -format UDZO -imagekey zlib-level=9 -o "$DMG"
codesign --force --timestamp --sign "$IDENTITY" "$DMG"

# --- Notarization ---------------------------------------------------------------
KEY_DIR=$WORK/private_keys
mkdir -p "$KEY_DIR"
ISSUER=$(python3 -c 'import json,os; print(json.loads(os.environ["APPLE_NOTARY_JSON"])["issuer_id"])')
KEY_ID=$(python3 -c 'import json,os; print(json.loads(os.environ["APPLE_NOTARY_JSON"])["key_id"])')
python3 - "$KEY_DIR/AuthKey_$KEY_ID.p8" <<'EOF'
import json, os, sys, textwrap
key = json.loads(os.environ["APPLE_NOTARY_JSON"])["private_key"].strip()
if "BEGIN PRIVATE KEY" not in key:
    key = "-----BEGIN PRIVATE KEY-----\n" + "\n".join(textwrap.wrap(key, 64)) + "\n-----END PRIVATE KEY-----"
open(sys.argv[1], "w").write(key + "\n")
EOF
chmod 600 "$KEY_DIR/AuthKey_$KEY_ID.p8"
set +e
OUT=$(xcrun notarytool submit "$DMG" --key "$KEY_DIR/AuthKey_$KEY_ID.p8" --key-id "$KEY_ID" --issuer "$ISSUER" --wait 2>&1)
set -e
echo "$OUT"
if ! echo "$OUT" | grep -q 'status: Accepted'; then
    ID=$(echo "$OUT" | awk '/^ *id:/{print $2; exit}')
    [ -n "$ID" ] && xcrun notarytool log "$ID" --key "$KEY_DIR/AuthKey_$KEY_ID.p8" --key-id "$KEY_ID" --issuer "$ISSUER" || true
    rm -rf "$KEY_DIR"
    echo "error: notarization failed" >&2
    exit 1
fi
rm -rf "$KEY_DIR"
xcrun stapler staple "$DMG"
xcrun stapler validate "$DMG"
security delete-keychain "$KEYCHAIN" || true
echo "Built $DMG"
