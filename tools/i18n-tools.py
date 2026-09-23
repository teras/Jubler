#!/usr/bin/env python3
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

"""Translation table maintenance, port of the Java `I18nTools`.

  extract  scan src/ for __("…") / N__("…") / tr("…") literals and rewrite
           resources/i18n-src/jubler-source.json ({"<english>": ""}, sorted)
  merge    bring every <lang>.json in line with the source: keep non-empty
           translations of keys that still exist, add "" for new keys, drop
           obsolete ones
  update   extract + merge, then regenerate the .ts files (i18n-json2ts.py)
"""
import json, os, re, subprocess, sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
SRC_DIR = os.path.join(ROOT, 'src')
I18N_SRC = os.path.join(ROOT, 'resources', 'i18n-src')
SOURCE_JSON = os.path.join(I18N_SRC, 'jubler-source.json')
LANGS = ['cs', 'de', 'el', 'es', 'fr', 'it', 'nl', 'pt', 'sr', 'tr']

LITERAL = r'"(?:[^"\\\n]|\\.)*"'
# The first argument of a translation call: one string literal, or adjacent
# literals that the compiler concatenates.
CALL = re.compile(r'(?<![A-Za-z0-9_])(?:N?__|tr)\s*\(\s*((?:' + LITERAL + r'\s*)+)')
ESCAPES = {'"': '"', 'n': '\n', 't': '\t', '\\': '\\', "'": "'"}


def decode(literal_run):
    out = []
    for lit in re.findall(LITERAL, literal_run):
        body = lit[1:-1]
        out.append(re.sub(r'\\(.)', lambda m: ESCAPES.get(m.group(1), '\\' + m.group(1)), body))
    return ''.join(out)


def extract():
    strings = set()
    for dirpath, _, files in os.walk(SRC_DIR):
        for name in files:
            if not name.endswith(('.cpp', '.h')):
                continue
            with open(os.path.join(dirpath, name), encoding='utf-8') as f:
                text = f.read()
            for m in CALL.finditer(text):
                s = decode(m.group(1))
                if s:
                    strings.add(s)
    write_json(SOURCE_JSON, {s: '' for s in strings})
    print('source: %d strings' % len(strings))


def read_table(path):
    if not os.path.exists(path):
        return {}
    with open(path, encoding='utf-8') as f:
        return json.load(f)   # duplicate keys: the last one wins


def write_json(path, table):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(dict(sorted(table.items())), f, ensure_ascii=False, indent=2)
        f.write('\n')


def merge():
    if not os.path.exists(SOURCE_JSON):
        print('Missing %s: run extract first' % SOURCE_JSON, file=sys.stderr)
        sys.exit(1)
    keys = read_table(SOURCE_JSON).keys()
    for lang in LANGS:
        path = os.path.join(I18N_SRC, lang + '.json')
        old = read_table(path)
        table = {}
        for k in keys:
            v = old.get(k)
            table[k] = v if isinstance(v, str) and v else ''   # JSON null counts as untranslated
        added = sum(1 for k in keys if k not in old)
        obsolete = sum(1 for k in old if k not in table)
        write_json(path, table)
        done = sum(1 for v in table.values() if v)
        pct = 100 * done // len(table) if table else 0
        print('%s: %d/%d translated (%d%%), +%d new, -%d obsolete' % (lang, done, len(table), pct, added, obsolete))


def update():
    extract()
    merge()
    sys.stdout.flush()
    subprocess.run([sys.executable, os.path.join(ROOT, 'tools', 'i18n-json2ts.py')], check=True)


COMMANDS = {'extract': extract, 'merge': merge, 'update': update}

if __name__ == '__main__':
    if len(sys.argv) != 2 or sys.argv[1] not in COMMANDS:
        print('Usage: i18n-tools.py extract|merge|update', file=sys.stderr)
        sys.exit(1)
    COMMANDS[sys.argv[1]]()
