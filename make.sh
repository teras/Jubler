#!/usr/bin/env bash

# (c) 2005-2025 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

# Be strict with script
set -euo pipefail

# ANSI escape codes for colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'  # No Color

# Get the directory where the script is located
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

dist_dir=$script_dir/dist
valid_targets=("windows" "linux" "linux-arm64" "generic" "macos" "macos-arm64" "all")

# KPacker configuration
kpacker_bin="$HOME/Works/System/bin/arch/linux-x86_64/kpacker"
if [ ! -x "$kpacker_bin" ]; then
    kpacker_bin=$(command -v kpacker || true)
fi
jubler_source_generic="$script_dir/build/jubler-generic"
jubler_source_linux64="$script_dir/build/jubler-linux64"
jubler_source_linuxarm64="$script_dir/build/jubler-linuxarm64"
jubler_source_macos="$script_dir/build/jubler-macos"
jubler_source_macosarm64="$script_dir/build/jubler-macosarm64"
jubler_source_win64="$script_dir/build/jubler-win64"
jubler_icon="$script_dir/resources/logo/logo.svg"
installer_icon="$script_dir/resources/logo/installer.svg"
subfile_icon="$script_dir/resources/logo/subfile.svg"
document_extensions="srt,vtt,ass,ssa,ttml,dfxp,itt,txt,sub,stl,xml,sbv"
document_name="Jubler subtitle file"

display_help() {
    echo -e "This is a helper script for building Jubler:"
    echo -e "  ${GREEN}build TARGET1[,TARGET2]${NC} Build Jubler for the list of provided targets."
    echo -e "  ${GREEN}winget X.Y.Z [--submit]${NC} Update WinGet manifest (dry-run by default)."
    echo -e "  ${GREEN}flatpak [--release]${NC}     Generate the manifest from its template and build (local, or release archive)."
    echo -e "  ${GREEN}clean${NC}                   Clean build files."
    echo -e "  ${GREEN}headers${NC}                 Check header files for copyright notice."
    echo -e "  ${GREEN}--help${NC}                  Display information about this script."
    echo
    echo -e "Available build targets:"
    (IFS=,; echo -e "  ${GREEN}${valid_targets[*]}${NC}")
    echo
    echo -e "Note: Signing and notarization are always performed when configured."
}

winget_action() {
    if [ $# -lt 2 ]; then
        echo -e "${RED}Error:${NC} Missing version argument for 'winget'. Usage: ./make.sh winget X.Y.Z [--submit]"
        exit 1
    fi

    local version=$2
    local submit_flag=""

    if [ "${3:-}" = "--submit" ]; then
        submit_flag="--submit"
    fi

    # Check if komac is installed
    if ! command -v komac &> /dev/null; then
        echo -e "${RED}Error:${NC} komac is not installed. Install it with: cargo install komac"
        exit 1
    fi

    local installer_url="https://github.com/teras/Jubler/releases/download/v${version}/Jubler-${version}-x64.exe"

    echo -e "${GREEN}Updating WinGet manifest for Jubler.App version ${version}...${NC}"
    echo -e "Installer URL: ${installer_url}"

    if [ -n "$submit_flag" ]; then
        echo -e "${GREEN}Will submit PR to microsoft/winget-pkgs${NC}"
        komac update Jubler.App --version "$version" --urls "$installer_url" --submit
    else
        echo -e "Dry run mode (use --submit to create PR)"
        komac update Jubler.App --version "$version" --urls "$installer_url" --dry-run
    fi
}


build_windows() {
    echo -e "${GREEN}Building for Windows...${NC}"
    cd "$script_dir"

    # Get version from environment variable or build.gradle.kts
    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Use separate temp directory for multi-target build mode
    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_windows"
    fi

    # Use KPacker to create the Windows installer
    "$kpacker_bin" --source="$jubler_source_win64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --single-classpath --target=WindowsX64 --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

    # Move result to final location if in multi-target build mode
    if [ "$build_multi_mode" = "true" ]; then
        mv "$output_dir"/Jubler-*-x64.exe "$dist_dir/"
        rm -rf "$output_dir"
    fi

    if [ -e "$dist_dir"/Jubler-*-x64.exe ]; then
        echo -e "${GREEN}Windows installer created successfully.${NC}"
    else
        echo -e "${RED}Error:${NC} Could not create Windows installer."
        exit 1
    fi
}

build_linux() {
    echo -e "${GREEN}Building for Linux...${NC}"
    cd "$script_dir"

    # Get version from environment variable or build.gradle.kts
    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Use separate temp directory for multi-target build mode
    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_linux"
    fi

    # Use KPacker to create the Linux x64 AppImage
    "$kpacker_bin" --source="$jubler_source_linux64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --single-classpath --target=LinuxX64 --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

    # Move result to final location if in multi-target build mode
    if [ "$build_multi_mode" = "true" ]; then
        mv "$output_dir"/Jubler-*-x86_64.AppImage "$dist_dir/"
        rm -rf "$output_dir"
    fi

    if [ -e "$dist_dir"/Jubler-*-x86_64.AppImage ]; then
        echo -e "${GREEN}Linux AppImage created successfully.${NC}"
    else
        echo -e "${RED}Error:${NC} Could not create Linux AppImage."
        exit 1
    fi
}

# Emit the AppStream <release> element for one Changelog.md section. The changelog keeps a flat
# list — one level of "- " bullets, optionally preceded by plain lines — which maps one to one onto
# the <ul>/<li> and <p> that AppStream allows (it has no nested lists). Plain lines are joined into a
# paragraph until a blank line; `code` and **bold** become the <code> and <em> AppStream accepts.
changelog_release_xml() {
    awk -v ver="$1" -v date="$2" '
        function inline(s) {
            gsub(/&/, "\\&amp;", s); gsub(/</, "\\&lt;", s); gsub(/>/, "\\&gt;", s)
            while (match(s, /`[^`]+`/))
                s = substr(s, 1, RSTART - 1) "<code>" substr(s, RSTART + 1, RLENGTH - 2) "</code>" substr(s, RSTART + RLENGTH)
            while (match(s, /\*\*[^*]+\*\*/))
                s = substr(s, 1, RSTART - 1) "<em>" substr(s, RSTART + 2, RLENGTH - 4) "</em>" substr(s, RSTART + RLENGTH)
            return s
        }
        function end_p() { if (para != "") { print "        <p>" inline(para) "</p>"; para = "" } }
        function end_ul() { if (inlist) { print "        </ul>"; inlist = 0 } }
        /^#### / { if (open) exit; if ($2 == ver) { open = 1
            print "    <release version=\"" ver "\" date=\"" date "\">"; print "      <description>" }
            next }
        !open { next }
        { line = $0; sub(/^[ \t]+/, "", line); sub(/[ \t]+$/, "", line) }
        line == "" { end_p(); next }
        line ~ /^[-*] / { end_p(); if (!inlist) { print "        <ul>"; inlist = 1 }
            print "          <li>" inline(substr(line, 3)) "</li>"; next }
        { end_ul(); para = (para == "" ? line : para " " line) }
        END { if (open) { end_p(); end_ul(); print "      </description>"; print "    </release>" } }
    ' "$script_dir/Changelog.md"
}

# The <release> list for the metainfo: the version being built, then the earlier feature releases
# (X.Y.0), four entries in all — the store reads the shown version from the first one, so a patch
# release must still lead. Dates come from the release tags, so nothing is typed by hand; a version
# not tagged yet, such as a local build ahead of its release, is dated today.
flatpak_releases_xml() {
    local base="${1%%-*}" picked=0 found=0 v date
    local versions
    versions=$(awk '/^#### [0-9]/ { print $2 }' "$script_dir/Changelog.md")
    if ! grep -qx "$base" <<< "$versions"; then
        echo -e "${RED}Error:${NC} Changelog.md has no '#### $base' section — Flathub requires release notes." >&2
        exit 1
    fi
    for v in $versions; do
        if [ "$found" = 0 ]; then
            [ "$v" = "$base" ] || continue     # sections newer than the one being built
            found=1
            date=$(git -C "$script_dir" for-each-ref --format='%(creatordate:short)' "refs/tags/v$v")
            [ -n "$date" ] || date=$(date +%F)
        else
            [[ "$v" =~ ^[0-9]+\.[0-9]+(\.0)?$ ]] || continue
            date=$(git -C "$script_dir" for-each-ref --format='%(creatordate:short)' "refs/tags/v$v")
            [ -n "$date" ] || continue         # an old release without a tag cannot be dated honestly
        fi
        changelog_release_xml "$v" "$date"
        picked=$((picked + 1))
        if [ "$picked" -ge 4 ]; then break; fi
    done
}

# Stage the Linux desktop metadata inside a distribution directory, as lib/flatpak/, so the release
# tarball carries it. Flathub requires the desktop file, metainfo and icon to be part of the upstream
# project rather than copies added to the submission pull request, and that tarball is what the
# manifest builds from. The icon is derived from the desktop logo here instead of being kept as a
# second file, so it cannot drift from it: artwork that runs to the canvas edge renders oversized
# beside its neighbours in an app grid, which the quality guidelines call out. Only the drawing is
# scaled — no path is touched, and the desktop installers keep the original edge-to-edge logo.
stage_flatpak_metadata() {
    local dest="$1/lib/flatpak" version="$2"
    mkdir -p "$dest"
    cp "$script_dir/resources/flatpak/com.panayotis.jubler.desktop" "$dest/"

    # The metainfo is a template: its release notes come from Changelog.md and its screenshots are
    # read at the release tag, so a release needs nothing edited by hand. An untagged local build
    # points the screenshots at the current commit instead.
    local ref="v${version%%-*}" releases="$dest/.releases"
    git -C "$script_dir" rev-parse -q --verify "refs/tags/$ref" > /dev/null \
        || ref=$(git -C "$script_dir" rev-parse --short=8 HEAD)
    flatpak_releases_xml "$version" > "$releases"
    sed -e "s|@@SCREENSHOT_REF@@|$ref|g" -e "/^@@RELEASES@@$/{
r $releases
d
}" "$script_dir/resources/flatpak/com.panayotis.jubler.metainfo.xml.in" > "$dest/com.panayotis.jubler.metainfo.xml"
    rm -f "$releases"
    if grep -q "@@" "$dest/com.panayotis.jubler.metainfo.xml"; then
        echo -e "${RED}Error:${NC} unfilled placeholder left in the generated metainfo."
        exit 1
    fi

    local logo="$script_dir/resources/logo/logo.svg"
    local icon_scale=0.84375        # leaves ~8% of the canvas free per side
    local viewbox offset
    viewbox=$(sed -n 's/.*viewBox="\([^"]*\)".*/\1/p' "$logo" | head -1)
    # Scaling happens about the origin, so shift by (1-scale) * centre to keep the drawing centred.
    offset=$(echo "$viewbox" | awk -v s=$icon_scale '{printf "%.3f", (1-s)*($1+$3/2)}')
    sed -e "0,/<svg /s|\(<svg [^>]*>\)|\1\n <g transform=\"translate($offset,$offset) scale($icon_scale)\">|" \
        -e "s|</svg>| </g>\n</svg>|" \
        "$logo" > "$dest/icon.svg"
    # sed on XML is only safe while the logo keeps its shape, so refuse to ship a silently unwrapped icon.
    if ! grep -q "scale($icon_scale)" "$dest/icon.svg" || ! grep -q "^ </g>$" "$dest/icon.svg"; then
        echo -e "${RED}Error:${NC} Could not wrap $logo for the Flatpak icon — has its markup changed?"
        exit 1
    fi
}

build_generic() {
    echo -e "${GREEN}Building for Generic...${NC}"
    cd "$script_dir"

    # Get version from environment variable or build.gradle.kts
    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Use separate temp directory for multi-target build mode
    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_generic"
    fi

    stage_flatpak_metadata "$jubler_source_generic" "$version"

    # Use KPacker to create Generic package
    "$kpacker_bin" --source="$jubler_source_generic/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --single-classpath --target=Generic --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

    # Move result to final location if in multi-target build mode
    if [ "$build_multi_mode" = "true" ]; then
        mv "$output_dir"/Jubler-*.tar.gz "$dist_dir/"
        rm -rf "$output_dir"
    fi

    if [ -e "$dist_dir"/Jubler-*.tar.gz ]; then
        echo -e "${GREEN}Generic package created successfully.${NC}"
    else
        echo -e "${RED}Error:${NC} Could not create Generic package."
        exit 1
    fi
}

build_macos() {
    echo -e "${GREEN}Building for MacOS...${NC}"
    cd "$script_dir"

    # Get version from environment variable or build.gradle.kts
    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Use separate temp directory for multi-target build mode
    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_macos"
    fi

    # Use KPacker to create macOS DMG with template (uncompressed for CI/CD signing)
    # KPacker auto-detects CI/CD environment and uses sudo when needed
    dmg_template="$script_dir/resources/installer/dmg_mac.zip"
    "$kpacker_bin" --source="$jubler_source_macos/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --single-classpath --target=MacX64 --icon="$jubler_icon" --install-icon="$installer_icon" --dmg-template="$dmg_template" --no-dmg-compress --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

    # Move result to final location if in multi-target build mode
    if [ "$build_multi_mode" = "true" ]; then
        mv "$output_dir"/Jubler-*.dmg "$dist_dir/"
        rm -rf "$output_dir"
    else
        # Clean up intermediate .app directory when building single target
        rm -rf "$dist_dir"/Jubler.app
    fi

    if [ -e "$dist_dir"/Jubler-*.dmg ]; then
        echo -e "${GREEN}macOS DMG (uncompressed) created successfully.${NC}"
    else
        echo -e "${RED}Error:${NC} Could not create macOS DMG."
        exit 1
    fi
}

build_linux_arm64() {
    echo -e "${GREEN}Building for Linux (arm64)...${NC}"
    cd "$script_dir"

    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_linux_arm64"
    fi

    # Use KPacker to create the Linux arm64 AppImage
    "$kpacker_bin" --source="$jubler_source_linuxarm64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --single-classpath --target=LinuxArm64 --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

    if [ "$build_multi_mode" = "true" ]; then
        mv "$output_dir"/Jubler-*-aarch64.AppImage "$dist_dir/"
        rm -rf "$output_dir"
    fi

    if [ -e "$dist_dir"/Jubler-*-aarch64.AppImage ]; then
        echo -e "${GREEN}Linux arm64 AppImage created successfully.${NC}"
    else
        echo -e "${RED}Error:${NC} Could not create Linux arm64 AppImage."
        exit 1
    fi
}

build_macos_arm64() {
    echo -e "${GREEN}Building for macOS (Apple Silicon)...${NC}"
    cd "$script_dir"

    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Always use a temp dir: the arm64 DMG is renamed so it does not clash with
    # the Intel DMG (both are produced as Jubler-<version>.dmg by KPacker).
    local output_dir="$dist_dir/temp_macos_arm64"

    dmg_template="$script_dir/resources/installer/dmg_mac.zip"
    "$kpacker_bin" --source="$jubler_source_macosarm64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --single-classpath --target=MacArm64 --icon="$jubler_icon" --install-icon="$installer_icon" --dmg-template="$dmg_template" --no-dmg-compress --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

    mv "$output_dir"/Jubler-*.dmg "$dist_dir/Jubler-${version}-arm64.dmg"
    rm -rf "$output_dir"

    if [ -e "$dist_dir/Jubler-${version}-arm64.dmg" ]; then
        echo -e "${GREEN}macOS arm64 DMG (uncompressed) created successfully.${NC}"
    else
        echo -e "${RED}Error:${NC} Could not create macOS arm64 DMG."
        exit 1
    fi
}

clean_action() {
    gradle clean
    rm -rf "$dist_dir/"
    rm -rf "$script_dir/build/"
}

check_headers() {
    cd $script_dir/modules
    for java_file in $(find . -name "*.java" | grep -v com/panayotis/jubler/subs/color/Quantize.java ) ; do
        if ! grep -q 'SPDX-License-Identifier' "$java_file"; then
            echo "${java_file}"
        fi
    done
}

build_action() {
    # Check if targets are provided
    if [ $# -lt 2 ]; then
        echo -e "${RED}Error:${NC} Missing targets for 'build'. Provide one or more targets."
        (IFS=,; echo -e "Valid build targets: ${valid_targets[*]}")
        exit 1
    fi

    mkdir -p dist

    targets=$2
    IFS=',' read -ra target_array <<< "$targets"

    # Set build_multi_mode if multiple targets are specified (including "all")
    build_multi_mode="false"
    if [ "${#target_array[@]}" -gt 1 ]; then
        build_multi_mode="true"
    fi
    for target in "${target_array[@]}"; do
        if [ "$target" = "all" ]; then
            build_multi_mode="true"
            break
        fi
    done

    # Regenerate the Jubler distribution fresh, exactly once per invocation.
    # gradle assembleDistribution produces every build/jubler-* platform dir in one
    # go, so a single build here serves all requested targets (single, multi or "all")
    # while guaranteeing every packaged installer is built from current code.
    echo -e "${GREEN}Building Jubler distribution...${NC}"
    cd "$script_dir"
    gradle clean assembleDistribution

    for target in "${target_array[@]}"; do
        case "$target" in
            "windows")
                build_windows
                ;;
            "linux")
                build_linux
                ;;
            "linux-arm64")
                build_linux_arm64
                ;;
            "generic")
                build_generic
                ;;
            "macos")
                build_macos
                ;;
            "macos-arm64")
                build_macos_arm64
                ;;
            "all")
                build_windows
                build_macos
                build_macos_arm64
                build_generic
                build_linux
                build_linux_arm64
                ;;
            *)
                echo -e "${RED}Error:${NC} Unknown build target: $target"
                (IFS=,; echo -e "Valid build targets: ${valid_targets[*]}")
                exit 1
                ;;
        esac
    done
}

# Generate the Flatpak manifest from resources/flatpak/com.panayotis.jubler.yml.in and build it.
# The template holds everything shared; only the app-source block is stamped here — a local build
# dir (fast iteration) or a GitHub release archive (reproducible, for Flathub). The generated
# manifest and its assets live in build/flatpak/ (volatile, git-ignored); nothing else diverges.
flatpak_action() {
    local mode="local"
    local install_flag="--install"
    local arch=""
    shift   # drop "flatpak"
    for arg in "$@"; do
        case "$arg" in
            --release) mode="release" ;;
            --no-install) install_flag="" ;;
            --arch=*) arch="${arg#--arch=}" ;;   # e.g. --arch=aarch64 (needs qemu binfmt on a foreign host)
        esac
    done

    cd "$script_dir"
    local version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Release mode consumes the published generic tarball, so nothing needs building locally.
    if [ "$mode" = "local" ]; then
        echo -e "${GREEN}Building Jubler distribution for Flatpak...${NC}"
        gradle assembleDistribution
        # Local builds consume build/jubler directly, so it needs the same metadata that
        # build_generic stages into the release tarball.
        stage_flatpak_metadata "$script_dir/build/jubler" "$version"
    fi

    local tpl="$script_dir/resources/flatpak/com.panayotis.jubler.yml.in"
    local gendir="$script_dir/build/flatpak"
    local manifest="$gendir/com.panayotis.jubler.yml"

    rm -rf "$gendir"
    mkdir -p "$gendir"
    # The only asset the manifest still references by bare name: the desktop file, metainfo and icon
    # now travel inside the distribution itself (see stage_flatpak_metadata), so that a Flathub
    # submission carries no copies of them.
    cp "$script_dir/resources/flatpak/vlc-ignore-time-for-cache.patch" "$gendir/"

    local block="$gendir/.appsource"
    if [ "$mode" = "release" ]; then
        # The generic release asset already is the distribution: a Jubler/ root holding lib/ and the
        # launcher, so flatpak-builder's default strip-components=1 lands exactly on lib/.
        local url="https://github.com/teras/Jubler/releases/download/v${version}/Jubler-${version}-generic.tar.gz"
        echo -e "${GREEN}Hashing release asset:${NC} $url"
        local hash
        hash=$(curl -fsSL "$url" | sha256sum | awk '{print $1}') || {
            echo -e "${RED}Error:${NC} Could not download $url — is the v${version} release published?"
            exit 1
        }
        cat > "$block" <<EOF
      # Published generic release asset (reproducible: URL + sha256).
      - type: archive
        url: ${url}
        sha256: ${hash}
EOF
    else
        cat > "$block" <<'EOF'
      # Local testing: the freshly built distribution (sibling of this generated manifest dir).
      - type: dir
        path: ../jubler
EOF
    fi

    # Stamp the @@APP_SOURCE@@ placeholder line with the chosen source block.
    sed "/^@@APP_SOURCE@@$/{
r $block
d
}" "$tpl" > "$manifest"
    rm -f "$block"
    echo -e "${GREEN}Generated manifest:${NC} $manifest"

    if [ "$mode" = "release" ]; then
        echo -e "Manifest is ready; copy it and its assets into the Flathub repository."
        return 0
    fi

    flatpak-builder --user $install_flag --force-clean --disable-rofiles-fuse \
        ${arch:+--arch=$arch} \
        --state-dir="$script_dir/.flatpak-builder" \
        "$script_dir/build/flatpak-build" "$manifest"
}

# Check if the script is called with an argument
if [ $# -eq 0 ]; then
    echo -e "${RED}Error:${NC} Missing parameter. Use --help for information."
    exit 1
fi

# Check the value of the first parameter
case "$1" in
    "--help")
        display_help
        ;;
    "build")
        build_action "$@"
        ;;
    "headers")
        check_headers
        ;;
    "clean")
        clean_action
        ;;
    "winget")
        winget_action "$@"
        ;;
    "flatpak")
        flatpak_action "$@"
        ;;
    *)
        echo -e "${RED}Error:${NC} Unknown parameter. Use --help for information."
        exit 1
        ;;
esac

exit 0

