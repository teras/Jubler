#!/usr/bin/env python3
# (c) 2005-2026 by Panayotis Katsaloulis
# SPDX-License-Identifier: AGPL-3.0-only
# This file is part of Jubler.

"""Finishes what macdeployqt leaves undone in Jubler.app: the libraries outside
Qt (libmpv, FFmpeg, hunspell and what they load in turn), which macdeployqt does
not follow reliably when they name each other through @rpath.

Every Mach-O file of the bundle is read with otool; each dependency that points
outside the bundle (Homebrew) or through an @rpath the bundle cannot satisfy is
copied into Contents/Frameworks and referred to as @rpath/<name>, and every file
gets the rpath that reaches Contents/Frameworks from where it lies. Repeated
until nothing is left to fix. Plugins that need a Qt module Jubler does not ship
(PDF, virtual keyboard) are removed. Finally everything is signed ad hoc, since
an Apple Silicon Mac does not run a modified, unsigned binary (the release
signing replaces it).

  bundle-libs.py <Jubler.app> <library folder>...
"""
import os
import shutil
import subprocess
import sys

app = os.path.abspath(sys.argv[1])
search = [os.path.abspath(p) for p in sys.argv[2:]]
contents = os.path.join(app, 'Contents')
frameworks = os.path.join(contents, 'Frameworks')
os.makedirs(frameworks, exist_ok=True)
SYSTEM = ('/System/', '/usr/lib/')
UNNEEDED = ('libqpdf.dylib', 'libqtvirtualkeyboardplugin.dylib')


def run(*cmd):
    return subprocess.run(cmd, check=True, capture_output=True, text=True).stdout


def is_macho(path):
    with open(path, 'rb') as f:
        magic = f.read(4)
    return magic in (b'\xcf\xfa\xed\xfe', b'\xce\xfa\xed\xfe', b'\xca\xfe\xba\xbe', b'\xbe\xba\xfe\xca')


def machos():
    for root, _, files in os.walk(contents):
        for name in files:
            path = os.path.join(root, name)
            if not os.path.islink(path) and os.path.isfile(path) and is_macho(path):
                yield path


def dependencies(path):
    lines = run('otool', '-L', path).splitlines()[1:]
    deps = [line.strip().split(' (compatibility')[0] for line in lines if line.strip()]
    if path.endswith('.dylib') or '.framework/' in path:
        deps = deps[1:]   # a library's first entry is its own id
    return deps


def rpath_to_frameworks(path):
    rel = os.path.relpath(frameworks, os.path.dirname(path))
    if os.path.join(contents, 'MacOS') == os.path.dirname(path):
        return '@executable_path/' + rel
    return '@loader_path' if rel == '.' else '@loader_path/' + rel


def rpaths(path):
    out, lines = [], run('otool', '-l', path).splitlines()
    for i, line in enumerate(lines):
        if 'cmd LC_RPATH' in line:
            out.append(lines[i + 2].strip().split(' ')[1])
    return out


def find_library(name):
    for folder in search:
        candidate = os.path.join(folder, name)
        if os.path.exists(candidate):
            return os.path.realpath(candidate)
    for folder in search:   # Homebrew keeps each formula's libraries under opt/<formula>/lib
        for root, _, files in os.walk(folder):
            if name in files:
                return os.path.realpath(os.path.join(root, name))
    return None


def in_bundle(dep, owner):
    """The file an @rpath dependency resolves to inside the bundle, if any."""
    rest = dep[len('@rpath/'):]
    for rp in rpaths(owner) + [rpath_to_frameworks(owner)]:
        base = rp.replace('@executable_path', os.path.join(contents, 'MacOS')).replace('@loader_path', os.path.dirname(owner))
        if os.path.exists(os.path.join(base, rest)):
            return True
    return False


for root, _, files in os.walk(os.path.join(contents, 'PlugIns')):
    for name in files:
        if name in UNNEEDED:
            os.remove(os.path.join(root, name))

missing = []
changed = True
while changed:
    changed = False
    for path in list(machos()):
        for dep in dependencies(path):
            if dep.startswith(SYSTEM) or dep.startswith('@executable_path') or dep.startswith('@loader_path'):
                continue
            if dep.startswith('@rpath/') and in_bundle(dep, path):
                continue
            name = os.path.basename(dep)
            if '.framework/' in dep:
                missing.append(f'{path}: {dep}')   # Qt frameworks are macdeployqt's
                continue
            target = os.path.join(frameworks, name)
            if not os.path.exists(target):
                source = dep if os.path.isabs(dep) and os.path.exists(dep) else find_library(name)
                if not source:
                    missing.append(f'{path}: {dep}')
                    continue
                shutil.copy2(source, target)
                os.chmod(target, 0o755)
                run('install_name_tool', '-id', '@rpath/' + name, target)
            run('install_name_tool', '-change', dep, '@rpath/' + name, path)
            if rpath_to_frameworks(path) not in rpaths(path):
                run('install_name_tool', '-add_rpath', rpath_to_frameworks(path), path)
            changed = True

if missing:
    sys.exit('error: dependencies that cannot be bundled:\n  ' + '\n  '.join(sorted(set(missing))))

# A copied library keeps the id it had in Homebrew (macdeployqt copies without
# changing it): each one is known by the name it is loaded with.
for name in os.listdir(frameworks):
    path = os.path.join(frameworks, name)
    if name.endswith('.dylib') and os.path.isfile(path) and not os.path.islink(path):
        if run('otool', '-D', path).splitlines()[-1].strip() != '@rpath/' + name:
            run('install_name_tool', '-id', '@rpath/' + name, path)

# Inside out: codesign refuses a bundle whose contents are not signed yet.
main = os.path.join(contents, 'MacOS')
for path in sorted(machos(), key=lambda p: -p.count(os.sep)):
    if os.path.dirname(path) != main:
        subprocess.run(['codesign', '--force', '--sign', '-', path], check=True, capture_output=True)
subprocess.run(['codesign', '--force', '--sign', '-', app], check=True, capture_output=True)
print('Bundled libraries into', frameworks)
