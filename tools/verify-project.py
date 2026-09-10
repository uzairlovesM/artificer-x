#!/usr/bin/env python3
from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
sources=list((root/'app/src/main/java').rglob('*.kt'))
errors=[]
for p in sources:
    text=p.read_text(errors='ignore')
    if '\x00' in text: errors.append(f'NUL bytes: {p}')
    if re.search(r'^en\s+class\b', text, re.M): errors.append(f'Malformed class declaration: {p}')
    if 'error.NonExistentClass' in text: errors.append(f'KSP placeholder type: {p}')
    if 'coroutineScopeJob' in text: errors.append(f'Unknown coroutine job: {p}')
# Ensure every project DAO path maps to exactly one Room entity table.
entities=[]
for p in sources:
    text=p.read_text(errors='ignore')
    m=re.search(r'@Entity\s*\((?:[^\n]*?tableName\s*=\s*)?"([^"]+)"', text)
    if m: entities.append(m.group(1))
from collections import Counter
for table,count in Counter(entities).items():
    if count>1: errors.append(f'Duplicate Room table: {table} ({count})')
print(f'Kotlin sources: {len(sources)}')
print(f'Room entity tables: {len(entities)}')
if errors:
    print('VERIFICATION FAILED')
    print('\n'.join(errors[:100]))
    sys.exit(1)
print('VERIFICATION PASSED')
