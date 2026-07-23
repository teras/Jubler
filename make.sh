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

    # Build Jubler distribution first if needed
    if [ ! -d "$jubler_source_win64" ]; then
        gradle clean assembleDistribution
    fi

    # Get version from environment variable or build.gradle.kts
    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Use separate temp directory for multi-target build mode
    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_windows"
    fi

    # Use KPacker to create the Windows installer
    "$kpacker_bin" --source="$jubler_source_win64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=WindowsX64 --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

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

    # Build Jubler distribution first if needed
    if [ ! -d "$jubler_source_linux64" ]; then
        gradle clean assembleDistribution
    fi

    # Get version from environment variable or build.gradle.kts
    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Use separate temp directory for multi-target build mode
    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_linux"
    fi

    # Use KPacker to create the Linux x64 AppImage
    "$kpacker_bin" --source="$jubler_source_linux64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=LinuxX64 --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

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

build_generic() {
    echo -e "${GREEN}Building for Generic...${NC}"
    cd "$script_dir"

    # Build Jubler distribution first if needed
    if [ ! -d "$jubler_source_generic" ]; then
        gradle clean assembleDistribution
    fi

    # Get version from environment variable or build.gradle.kts
    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Use separate temp directory for multi-target build mode
    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_generic"
    fi

    # Use KPacker to create Generic package
    "$kpacker_bin" --source="$jubler_source_generic/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=Generic --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

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

    # Build Jubler distribution first if needed
    if [ ! -d "$jubler_source_macos" ]; then
        gradle clean assembleDistribution
    fi

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
    "$kpacker_bin" --source="$jubler_source_macos/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=MacX64 --icon="$jubler_icon" --install-icon="$installer_icon" --dmg-template="$dmg_template" --no-dmg-compress --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

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

    if [ ! -d "$jubler_source_linuxarm64" ]; then
        gradle clean assembleDistribution
    fi

    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    local output_dir="$dist_dir"
    if [ "$build_multi_mode" = "true" ]; then
        output_dir="$dist_dir/temp_linux_arm64"
    fi

    # Use KPacker to create the Linux arm64 AppImage
    "$kpacker_bin" --source="$jubler_source_linuxarm64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=LinuxArm64 --icon="$jubler_icon" --install-icon="$installer_icon" --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

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

    if [ ! -d "$jubler_source_macosarm64" ]; then
        gradle clean assembleDistribution
    fi

    version=${JUBLER_VERSION:-$(gradle properties -q | grep "^version:" | awk '{print $2}')}

    # Always use a temp dir: the arm64 DMG is renamed so it does not clash with
    # the Intel DMG (both are produced as Jubler-<version>.dmg by KPacker).
    local output_dir="$dist_dir/temp_macos_arm64"

    dmg_template="$script_dir/resources/installer/dmg_mac.zip"
    "$kpacker_bin" --source="$jubler_source_macosarm64/lib" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=MacArm64 --icon="$jubler_icon" --install-icon="$installer_icon" --dmg-template="$dmg_template" --no-dmg-compress --document-extensions="$document_extensions" --document-name="$document_name" --document-icon="$subfile_icon"

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
    *)
        echo -e "${RED}Error:${NC} Unknown parameter. Use --help for information."
        exit 1
        ;;
esac

exit 0

