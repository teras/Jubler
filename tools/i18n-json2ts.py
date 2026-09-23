#!/usr/bin/env python3
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

"""Convert the flat JSON translation tables (English source → translation)
into Qt .ts files with the single "jubler" context used by __()."""
import json, sys, os, html

SRC = os.path.join(os.path.dirname(__file__), '..', 'resources', 'i18n-src')
OUT = os.path.join(os.path.dirname(__file__), '..', 'resources', 'i18n')
LANGS = ['cs', 'de', 'el', 'es', 'fr', 'it', 'nl', 'pt', 'sr', 'tr']

def esc(s):
    return html.escape(s, quote=False)

def convert(lang):
    with open(os.path.join(SRC, lang + '.json'), encoding='utf-8') as f:
        table = json.load(f)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<!DOCTYPE TS>',
             '<TS version="2.1" language="%s">' % lang, '<context>', '    <name>jubler</name>']
    for key in sorted(table):
        value = table[key]
        lines.append('    <message>')
        lines.append('        <source>%s</source>' % esc(key))
        if value:
            lines.append('        <translation>%s</translation>' % esc(value))
        else:
            lines.append('        <translation type="unfinished"></translation>')
        lines.append('    </message>')
    lines += ['</context>', '</TS>', '']
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, 'jubler_%s.ts' % lang), 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
    print('%s: %d strings, %d translated' % (lang, len(table), sum(1 for v in table.values() if v)))

for lang in (sys.argv[1:] or LANGS):
    convert(lang)
