#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path('app/src/main/java')
failures = []

kt_files = list(root.rglob('*.kt'))
if not kt_files:
    failures.append('no Kotlin source files found')

for path in kt_files:
    text = path.read_text(encoding='utf-8', errors='ignore')
    if '{{' in text:
        failures.append(f'malformed double-brace syntax: {path}')
    stripped = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    stripped = re.sub(r'(^|\s)//.*', r'\1', stripped)
    if not stripped.strip():
        failures.append(f'comment-only Kotlin source: {path}')

# The deep AI layer previously used repeated exact helper declarations that
# collided during Kotlin compilation. Catch that regression before KSP.
for path in (root / 'com/waheed/artificerx/core/ai/apex/deep').rglob('*.kt'):
    text = path.read_text(encoding='utf-8', errors='ignore')
    decls = re.findall(r'private fun Double\.tanhSafe\(\): Double', text)
    if len(decls) > 1:
        failures.append(f'duplicate tanhSafe declarations: {path}')

# Guard the expected real implementation footprint: very large generated
# files must contain executable declarations and not degrade to comment noise.
for path in (root / 'com/waheed/artificerx/core/ai/apex/deep').rglob('*.kt'):
    text = path.read_text(encoding='utf-8', errors='ignore')
    functions = len(re.findall(r'\bfun\s+[A-Za-z_]', text))
    if len(text.splitlines()) > 200 and functions < 3:
        failures.append(f'low executable density in deep file: {path}')

if failures:
    print('DEEP PROJECT VERIFICATION FAILED')
    for failure in failures:
        print(f' - {failure}')
    sys.exit(1)

print(f'DEEP PROJECT VERIFICATION PASSED: {len(kt_files)} Kotlin files scanned')
