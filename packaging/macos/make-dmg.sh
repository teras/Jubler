#!/bin/bash
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

# The macOS disk image: Jubler.app built against Homebrew's Qt and mpv (so it
# needs the macOS version of the build machine, 15 or newer), everything it loads
# copied into the bundle, signed with the Developer ID, put into the Java
# release's styled drag-to-Applications image, notarized and stapled.
#   make-dmg.sh <version> <arch suffix: "" or "-arm64">
# Signing needs MACOS_CERTIFICATE (base64 .p12), MACOS_CERTIFICATE_PWD and
# APPLE_NOTARY_JSON ({issuer_id, key_id, private_key}); there is no unsigned result.
set -euo pipefail

VERSION=$1
SUFFIX=$2
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
WORK=$ROOT/build-macos
APP=$WORK/Jubler.app
DMG=$ROOT/dist/Jubler-$VERSION$SUFFIX.dmg
QT=$(brew --prefix qt)

for v in MACOS_CERTIFICATE MACOS_CERTIFICATE_PWD APPLE_NOTARY_JSON; do
    [ -n "${!v:-}" ] || { echo "error: $v is not set; a release is never left unsigned" >&2; exit 1; }
done

# --- Build --------------------------------------------------------------------
cmake -S "$ROOT" -B "$WORK" -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_OSX_DEPLOYMENT_TARGET=15.0 \
      -DCMAKE_PREFIX_PATH="$QT"
cmake --build "$WORK"
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
"$QT/bin/macdeployqt" "$APP" -verbose=1
# What macdeployqt leaves outside: the non-Qt libraries (Homebrew's lib, then its Cellar for keg-only ones).
python3 "$ROOT/packaging/macos/bundle-libs.py" "$APP" "$(brew --prefix)/lib" "$(brew --prefix)/Cellar"
# Nothing may still point outside the bundle and the system.
leaks=$(find "$APP" -type f \( -perm +111 -o -name '*.dylib' \) -exec otool -L {} \; 2>/dev/null \
        | grep -E '^\s+(/opt/homebrew|/usr/local)/' | sort -u || true)
if [ -n "$leaks" ]; then
    echo "error: the bundle still loads libraries from outside it:" >&2
    echo "$leaks" >&2
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
