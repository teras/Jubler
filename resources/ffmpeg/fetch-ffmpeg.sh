#!/usr/bin/env bash

# (c) 2005-2025 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.
#
# Downloads the ffmpeg + ffprobe executables for a given KPacker target and
# places them (plus the ffmpeg license) into a destination directory, so they
# can be bundled next to jubler.jar in the self-contained packages. Jubler finds
# them at runtime through SystemFileFinder.AppPath (see FFmpegAudioPreview).
#
# The bundled ffmpeg builds are GPL; Jubler is AGPL-3.0 (GPL-compatible). The
# license text is shipped alongside the binaries as FFMPEG-LICENSE.txt.
#
# Usage:  fetch-ffmpeg.sh <KPackerTarget> <dest-dir>
#   KPackerTarget: WindowsX64 | LinuxX64 | LinuxArm64 | MacX64 | MacArm64
#
# Each download URL can be overridden with an environment variable so a release
# can pin an exact build instead of tracking "latest":
#   FFMPEG_WINDOWSX64_URL, FFMPEG_LINUXX64_URL, FFMPEG_LINUXARM64_URL,
#   FFMPEG_MACX64_URL, FFPROBE_MACX64_URL, FFMPEG_MACARM64_URL, FFPROBE_MACARM64_URL

set -euo pipefail

target="${1:-}"
dest="${2:-}"
if [ -z "$target" ] || [ -z "$dest" ]; then
    echo "Usage: $0 <WindowsX64|LinuxX64|LinuxArm64|MacX64|MacArm64> <dest-dir>" >&2
    exit 1
fi
mkdir -p "$dest"

# BtbN builds (GPL) ship ffmpeg, ffprobe and ffplay for Windows and Linux.
BTBN="https://github.com/BtbN/FFmpeg-Builds/releases/download/latest"
# macOS static builds: evermeet.cx (x86_64) and osxexperts.net (arm64) ship the
# binaries individually. osxexperts URLs are versioned; override if they move.
EVERMEET="https://evermeet.cx/ffmpeg/getrelease"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

fetch() { curl -fSL --retry 3 -o "$2" "$1"; }

extract_from_archive() {
    # $1 archive, $2 'zip'|'tarxz', $3 exe-suffix
    local archive="$1" kind="$2" sfx="$3"
    if [ "$kind" = "zip" ]; then
        unzip -q "$archive" -d "$tmp/ex"
    else
        mkdir -p "$tmp/ex"; tar -xJf "$archive" -C "$tmp/ex"
    fi
    # The BtbN archives nest the binaries under <name>/bin/
    local f
    for tool in ffmpeg ffprobe; do
        f="$(find "$tmp/ex" -type f -name "${tool}${sfx}" | head -n1)"
        [ -n "$f" ] || { echo "ERROR: ${tool}${sfx} not found in $archive" >&2; exit 1; }
        cp "$f" "$dest/${tool}${sfx}"
        chmod +x "$dest/${tool}${sfx}"
    done
}

case "$target" in
    WindowsX64)
        url="${FFMPEG_WINDOWSX64_URL:-$BTBN/ffmpeg-master-latest-win64-gpl.zip}"
        fetch "$url" "$tmp/ff.zip"
        extract_from_archive "$tmp/ff.zip" zip ".exe"
        ;;
    LinuxX64)
        url="${FFMPEG_LINUXX64_URL:-$BTBN/ffmpeg-master-latest-linux64-gpl.tar.xz}"
        fetch "$url" "$tmp/ff.tar.xz"
        extract_from_archive "$tmp/ff.tar.xz" tarxz ""
        ;;
    LinuxArm64)
        url="${FFMPEG_LINUXARM64_URL:-$BTBN/ffmpeg-master-latest-linuxarm64-gpl.tar.xz}"
        fetch "$url" "$tmp/ff.tar.xz"
        extract_from_archive "$tmp/ff.tar.xz" tarxz ""
        ;;
    MacX64)
        fetch "${FFMPEG_MACX64_URL:-$EVERMEET/ffmpeg/zip}"  "$tmp/ffmpeg.zip"
        fetch "${FFPROBE_MACX64_URL:-$EVERMEET/ffprobe/zip}" "$tmp/ffprobe.zip"
        unzip -q "$tmp/ffmpeg.zip"  -d "$dest"
        unzip -q "$tmp/ffprobe.zip" -d "$dest"
        chmod +x "$dest/ffmpeg" "$dest/ffprobe"
        ;;
    MacArm64)
        # osxexperts.net publishes arm64 static builds; pin via the env overrides.
        : "${FFMPEG_MACARM64_URL:?Set FFMPEG_MACARM64_URL to an arm64 macOS ffmpeg zip}"
        : "${FFPROBE_MACARM64_URL:?Set FFPROBE_MACARM64_URL to an arm64 macOS ffprobe zip}"
        fetch "$FFMPEG_MACARM64_URL"  "$tmp/ffmpeg.zip"
        fetch "$FFPROBE_MACARM64_URL" "$tmp/ffprobe.zip"
        unzip -q "$tmp/ffmpeg.zip"  -d "$dest"
        unzip -q "$tmp/ffprobe.zip" -d "$dest"
        chmod +x "$dest/ffmpeg" "$dest/ffprobe"
        ;;
    *)
        echo "ERROR: unknown target '$target'" >&2
        exit 1
        ;;
esac

# Ship the ffmpeg license next to the binaries (GPL compliance).
cat > "$dest/FFMPEG-LICENSE.txt" <<'EOF'
The bundled ffmpeg and ffprobe executables are GPL builds of FFmpeg
(https://ffmpeg.org). FFmpeg is free software licensed under the GNU General
Public License version 3. The corresponding source code is available from
https://ffmpeg.org/download.html and from the respective build providers.
EOF

echo "ffmpeg + ffprobe for $target placed in $dest"
