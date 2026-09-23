#!/bin/bash
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

# Files to exclude from header check (third-party code)
declare -a EXCLUDES=(
)

# Resolve repository root relative to this script's location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT" || exit 1

# Check all source files
missing_header=0
while IFS= read -r file; do
    # Check if file is in EXCLUDES
    excluded=0
    for exclude in "${EXCLUDES[@]}"; do
        if [[ "$file" == "$exclude" ]]; then
            excluded=1
            break
        fi
    done

    if [[ $excluded -eq 1 ]]; then
        continue
    fi

    # Check if file has SPDX-License-Identifier
    if ! grep -q "SPDX-License-Identifier" "$file"; then
        echo "$file"
        missing_header=1
    fi
done < <(git ls-files | grep -E '\.(cpp|h|py|java)$')

exit $missing_header
