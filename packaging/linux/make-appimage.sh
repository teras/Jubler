#!/bin/sh
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

# The Linux AppImage: built on Arch Linux (current Qt and mpv) and made portable
# with quick-sharun, which bundles every library including the C library, so it
# runs on any distribution. Runs in the pkgforge-dev Arch container prepared by
# anylinux-setup-action (quick-sharun on the PATH): make-appimage.sh <version>
set -eu

VERSION=$1
ARCH=$(uname -m)
export ARCH VERSION

pacman -Syu --noconfirm --needed git base-devel cmake ninja pkgconf python \
    qt6-base qt6-svg qt6-tools qt6-translations qt6-wayland mpv ffmpeg hunspell openssl zlib

cmake -S . -B build-appimage -G Ninja -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr
cmake --build build-appimage
cmake --install build-appimage

export OUTPATH=./dist
export OUTNAME="Jubler-$VERSION-$ARCH.AppImage"
export DESKTOP=/usr/share/applications/com.panayotis.jubler.desktop
export ICON=/usr/share/icons/hicolor/scalable/apps/com.panayotis.jubler.svg

quick-sharun /usr/bin/jubler
quick-sharun --make-appimage
quick-sharun --test "./dist/$OUTNAME"
