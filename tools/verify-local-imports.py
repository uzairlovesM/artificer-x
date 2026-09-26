#!/usr/bin/env python3
"""Conservative local-import contract for Artificer-X Kotlin/Java sources."""
from __future__ import annotations
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SRC=[ROOT/'app/src/main/java', ROOT/'baselineprofile/src/main/java', ROOT/'benchmark/src/main/java']
DECL=defaultdict(set)
PKGS=set()

def declaration_names(text: str):
    # Annotations may sit immediately above declarations. Strip annotation-only
    # prefixes so @Composable fun Foo and @JvmStatic fun Foo are indexed.
    text=re.sub(r'@[A-Za-z0-9_.:]+(?:\([^\n)]*\))?\s*', '', text)
    patterns=[
        r'^(?:(?:public|private|protected|internal|final|open|abstract|sealed|data|value|expect|actual|annotation|const)\s+)*'
        r'(?:class|interface|object|enum class|typealias|fun|val|var)\s+(?:[A-Za-z_][A-Za-z0-9_<>?, .]*\.)?([A-Za-z_][A-Za-z0-9_]*)',
    ]
    rx=re.compile(patterns[0],re.M)
    return {m.group(1) for m in rx.finditer(text)}

for base in SRC:
    if not base.exists(): continue
    for p in base.rglob('*'):
        if p.suffix not in {'.kt','.java'}: continue
        t=p.read_text(encoding='utf-8')
        m=re.search(r'^package\s+([A-Za-z0-9_.]+)',t,re.M)
        if not m: continue
        pkg=m.group(1); PKGS.add(pkg)
        DECL[pkg].update(declaration_names(t))

errors=[]
for base in SRC:
    if not base.exists(): continue
    for p in base.rglob('*.kt'):
        t=p.read_text(encoding='utf-8')
        for line in t.splitlines():
            m=re.match(r'^\s*import\s+(com\.waheed\.artificerx(?:\.[A-Za-z0-9_]+)*)\.([A-Za-z_*][A-Za-z0-9_*]*)\s*$',line)
            if not m: continue
            pkg,name=m.group(1),m.group(2)
            if name=='*' or name in {'R','BuildConfig'}:
                continue
            # Ignore imports whose package is not represented in this project;
            # those may be generated or produced by another source processor.
            if pkg not in PKGS:
                continue
            if name not in DECL[pkg]:
                errors.append(f'{p}: local import not found: {pkg}.{name}')

if errors:
    print('::error::Local import contract failed')
    for e in errors[:200]: print(e)
    if len(errors)>200: print(f'... and {len(errors)-200} more')
    sys.exit(1)
print('LOCAL IMPORT CONTRACT: PASS')
print(f'packages={len(PKGS)} indexed_symbols={sum(len(v) for v in DECL.values())}')
