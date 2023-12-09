#!/usr/bin/env bash

# (c) 2005-2023 by Panayotis Katsaloulis
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

display_help() {
    echo -e "This is a helper script for building Jubler:"
    echo -e "  ${GREEN}version X.Y.Z${NC}           Update Jubler version."
    echo -e "  ${GREEN}build TARGET1[,TARGET2]${NC} Build Jubler for the list of provided targets."
    echo -e "  ${GREEN}clean${NC}                   Clean build files."
    echo -e "  ${GREEN}headers${NC}                 Check header files for copyright notice."
    echo -e "  ${GREEN}--help${NC}                  Display information about this script."
    echo
    echo -e "Available build targets:"
    echo -e "  ${GREEN}windows, linux, generic, macos, all${NC}"
    echo
    echo -e "Additional parameters for specific targets:"
    echo -e "  ${GREEN}notarize${NC}               Perform notarization for MacOS target."
    echo -e "  ${GREEN}nosign${NC}                 Skip signing for Windows and MacOS targets."
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
    # Check if extra arguments are provided
    NOSIGN=$(if [[ $* == *"nosign"* ]]; then echo ",nosign"; else echo ""; fi)
    mvn clean install -Pdist,win64${NOSIGN}
    cd "$script_dir/modules/installer/target" || exit
    if [ -e Jubler-*.x32.exe ]; then
        echo -e "${GREEN}Copying x32 EXE file to dist.${NC}"
        cp Jubler-*.x32.exe "$dist_dir/"
    fi
    if [ -e Jubler-*.x64.exe ]; then
        echo -e "${GREEN}Copying x64 EXE file to dist.${NC}"
        cp Jubler-*.x64.exe "$dist_dir/"
    fi
    if [ ! -e Jubler-*.x32.exe ] && [ ! -e Jubler-*.x64.exe ]; then
        echo -e "${RED}Error:${NC} Could not find x32 or x64 EXE file for Jubler."
        exit 1
    fi
}

build_linux() {
    echo -e "${GREEN}Building for Linux...${NC}"
    cd "$script_dir"
    mvn clean install -Pdist,linux
    cd "$script_dir/modules/installer/target" || exit
    if [ -e Jubler-*.appimage ]; then
        echo -e "${GREEN}Copying AppImage file to dist.${NC}"
        cp Jubler-*.appimage "$dist_dir/"
    else
        echo -e "${RED}Error:${NC} Could not find AppImage file for Jubler."
        exit 1
    fi
}

build_generic() {
    echo -e "${GREEN}Building for Generic...${NC}"
    cd "$script_dir"
    mvn clean install -Pdist,generic
    cd "$script_dir/modules/installer/target" || exit
    if [ -e Jubler-*.tar.bz2 ]; then
        echo -e "${GREEN}Copying TAR.BZ2 file to dist.${NC}"
        cp Jubler-*.tar.bz2 "$dist_dir/"
    else
        echo -e "${RED}Error:${NC} Could not find TAR.BZ2 file for Jubler."
        exit 1
    fi
    cd $script_dir
}

build_macos() {
    echo -e "${GREEN}Building for MacOS...${NC}"
    cd "$script_dir"
    # Check if extra arguments are provided
    NOTARIZE=$(if [[ $* == *"notarize"* ]]; then echo ",notarize"; else echo ""; fi)
    NOSIGN=$(if [[ $* == *"nosign"* ]]; then echo ",nosign"; else echo ""; fi)
    mvn clean install -Pdist,macos${NOTARIZE}${NOSIGN}
    cd "$script_dir/modules/installer/target" || exit
    if [ -e Jubler-*.dmg ]; then
        echo -e "${GREEN}Copying DMG file to dist.${NC}"
        cp Jubler-*.dmg "$dist_dir/"
    else
        if [ -e Jubler-*.zip ]; then
            echo -e "${GREEN}Copying ZIP file to dist.${NC}"
            cp Jubler-*.zip "$dist_dir/"
        else
            echo -e "${RED}Error:${NC} Could not find DMG or ZIP file for Jubler."
            exit 1
        fi
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
        echo -e "Valid build targets: ${valid_targets[@]}"
        exit 1
    fi

    mkdir -p dist

    targets=$2
    IFS=',' read -ra target_array <<< "$targets"

    for target in "${target_array[@]}"; do
        case "$target" in
            "windows")
                build_windows "$@"
                ;;
            "linux")
                build_linux
                ;;
            "generic")
                build_generic
                ;;
            "macos")
                build_macos "$@"
                ;;
            "all")
                build_windows "$@"
                build_linux
                build_generic
                build_macos "$@"
                ;;
            *)
                echo -e "${RED}Error:${NC} Unknown build target: $target"
                echo -e "Valid build targets: ${valid_targets[@]}"
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

