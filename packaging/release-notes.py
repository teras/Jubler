#!/usr/bin/env python3
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

"""The body of a GitHub release: the version's section of Changelog.md.

The update check shows these notes to the users of older versions (and skips a
release without them). A pre-release (a version with a suffix, 11.0.0-alpha)
takes the notes of its version and the warning the Java releases carried.

  release-notes.py <version>   (prints the Markdown)
"""
import pathlib
import re
import sys

version = sys.argv[1]
base = version.split('-')[0]
changelog = (pathlib.Path(__file__).resolve().parent.parent / 'Changelog.md').read_text(encoding='utf-8')

section = re.search(r'^#### ' + re.escape(base) + r'\s*\n(.*?)(?=^#### |\Z)', changelog, re.M | re.S)
if not section or not section.group(1).strip():
    sys.exit(f'error: Changelog.md has no notes for {base}')

notes = section.group(1).strip()
if version != base:
    notes = ('THIS IS AN ALPHA VERSION' if 'alpha' in version else 'THIS IS A PRE-RELEASE VERSION') + \
            '\n\nNOT FOR PRODUCTION OR REDISTRIBUTION - USE WITH CARE\n\n' + notes
print(notes)
