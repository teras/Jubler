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
valid_targets=("windows" "linux" "generic" "macos" "all")

# KPacker configuration
kpacker_bin="$HOME/Works/System/bin/arch/linux-x86_64/kpacker"
jubler_source="$script_dir/modules/installer/target/jubler/lib"
jubler_icon="$script_dir/resources/logo/newlogo.svg"

display_help() {
    echo -e "This is a helper script for building Jubler:"
    echo -e "  ${GREEN}version X.Y.Z${NC}           Update Jubler version."
    echo -e "  ${GREEN}build TARGET1[,TARGET2]${NC} Build Jubler for the list of provided targets."
    echo -e "  ${GREEN}clean${NC}                   Clean build files."
    echo -e "  ${GREEN}headers${NC}                 Check header files for copyright notice."
    echo -e "  ${GREEN}--help${NC}                  Display information about this script."
    echo
    echo -e "Available build targets:"
    (IFS=,; echo -e "  ${GREEN}${valid_targets[*]}${NC}")
    echo
    echo -e "Note: Signing and notarization are always performed when configured."
}

version_action() {
    local version=$2
    if [ $# -lt 2 ]; then
        echo -e "${RED}Error:${NC} Missing argument for 'version'. Provide a value in X.Y.Z form."
        exit 1
    fi
    if ! [[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9_]+)?$ ]]; then
        echo -e "${RED}Error:${NC} Invalid version format."
        exit 1
    fi
    local version=$2
    cd $script_dir
    mvn versions:set -DnewVersion=$version -DgenerateBackupPoms=false -DprocessAllModules
#    mkdir -p "$dist_dir/"
#    if [ ! -e "$dist_dir/.Komac.jar" ]; then
#        wget -O "$dist_dir/.Komac.jar" https://github.com/russellbanks/Komac/releases/download/v1.11.0/Komac-1.11.0-all.jar
#    fi
#    cd resources/winget/jubler/manifests/j/Jubler/
#    java -jar $dist_dir/.Komac.jar update --version=$version
}

build_windows() {
    echo -e "${GREEN}Building for Windows...${NC}"
    cd "$script_dir"

    # Build Jubler JAR first if needed
    if [ ! -d "$jubler_source" ]; then
        mvn clean install
    fi

    # Get version from POM
    version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

    # Use separate temp directory for build all mode
    local output_dir="$dist_dir"
    if [ "$build_all_mode" = "true" ]; then
        output_dir="$dist_dir/temp_windows"
    fi

    # Use KPacker to create Windows installer
    "$kpacker_bin" --source="$jubler_source" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=WindowsX64 --icon="$jubler_icon"

    # Move result to final location if in build all mode
    if [ "$build_all_mode" = "true" ]; then
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

    # Build Jubler JAR first if needed
    if [ ! -d "$jubler_source" ]; then
        mvn clean install
    fi

    # Get version from POM
    version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

    # Use separate temp directory for build all mode
    local output_dir="$dist_dir"
    if [ "$build_all_mode" = "true" ]; then
        output_dir="$dist_dir/temp_linux"
    fi

    # Use KPacker to create Linux x64 AppImage
    "$kpacker_bin" --source="$jubler_source" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=LinuxX64 --icon="$jubler_icon"

    # Move result to final location if in build all mode
    if [ "$build_all_mode" = "true" ]; then
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

    # Build Jubler JAR first if needed
    if [ ! -d "$jubler_source" ]; then
        mvn clean install
    fi

    # Get version from POM
    version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

    # Use separate temp directory for build all mode
    local output_dir="$dist_dir"
    if [ "$build_all_mode" = "true" ]; then
        output_dir="$dist_dir/temp_generic"
    fi

    # Use KPacker to create Generic package
    "$kpacker_bin" --source="$jubler_source" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=Generic --icon="$jubler_icon"

    # Move result to final location if in build all mode
    if [ "$build_all_mode" = "true" ]; then
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

    # Build Jubler JAR first if needed
    if [ ! -d "$jubler_source" ]; then
        mvn clean install
    fi

    # Get version from POM
    version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

    # Use separate temp directory for build all mode
    local output_dir="$dist_dir"
    if [ "$build_all_mode" = "true" ]; then
        output_dir="$dist_dir/temp_macos"
    fi

    # Use KPacker to create macOS DMG with template
    dmg_template="$script_dir/modules/installer/resources/dmg_mac.zip"
    "$kpacker_bin" --source="$jubler_source" --out="$output_dir" --name=Jubler --version="$version" --mainjar=jubler.jar --target=MacX64 --icon="$jubler_icon" --dmg-template="$dmg_template"

    # Move result to final location if in build all mode
    if [ "$build_all_mode" = "true" ]; then
        mv "$output_dir"/Jubler-*.dmg "$dist_dir/"
        rm -rf "$output_dir"
    fi

    if [ -e "$dist_dir"/Jubler-*.dmg ]; then
        echo -e "${GREEN}macOS DMG created successfully.${NC}"
    else
        echo -e "${RED}Error:${NC} Could not create macOS DMG."
        exit 1
    fi
}

clean_action() {
    mvn clean
    rm -rf "$dist_dir/"
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

    # Set build_all_mode if "all" is in the targets
    build_all_mode="false"
    for target in "${target_array[@]}"; do
        if [ "$target" = "all" ]; then
            build_all_mode="true"
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
            "generic")
                build_generic
                ;;
            "macos")
                build_macos
                ;;
            "all")
                build_windows
                build_macos
                build_generic
                build_linux
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
    "version")
        version_action "$@"
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
    *)
        echo -e "${RED}Error:${NC} Unknown parameter. Use --help for information."
        exit 1
        ;;
esac

exit 0

