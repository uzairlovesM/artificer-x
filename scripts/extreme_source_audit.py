#!/usr/bin/env python3
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
java = ROOT / 'app/src/main/java'
kt = list(java.rglob('*.kt'))
text = '\n'.join(p.read_text(errors='ignore') for p in kt)

checks = []
checks.append(('placeholder refs', not bool(re.search(r'PlaceholderScreen|TODO|FIXME', text))))
checks.append(('unsafe clear history load bug absent', 'bitmapStore.clearHistory()\n                    bitmapStore.loadProject' not in text))
checks.append(('workspace filesystem', (java/'com/waheed/artificerx/core/storage/WorkspaceFileSystem.kt').exists()))
checks.append(('permission manager', (java/'com/waheed/artificerx/core/permissions/PermissionManager.kt').exists()))
checks.append(('automation engine', (java/'com/waheed/artificerx/core/automation/AutomationEngine.kt').exists()))
checks.append(('custom brush designer', (java/'com/waheed/artificerx/ui/screens/art/CustomBrushDesignerScreen.kt').exists()))
checks.append(('system observatory', (java/'com/waheed/artificerx/ui/screens/system/SystemObservatoryScreen.kt').exists()))
checks.append(('graphics path dependency', 'androidx-graphics-path' in (ROOT/'gradle/libs.versions.toml').read_text()))
checks.append(('media3 transformer bundle', 'media3-transformer' in (ROOT/'gradle/libs.versions.toml').read_text()))
route_text = (java/'com/waheed/artificerx/ui/navigation/Destinations.kt').read_text()
routes = re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', route_text)
checks.append(('routes unique', len(routes) == len({name for name, _ in routes}) and len(routes) == len({value for _, value in routes})))

# crude source balance, useful for catching edit damage before a real build
for p in kt:
    s=p.read_text(errors='ignore')
    # Strip quoted strings and line comments before the coarse brace check;
    # security patterns can legitimately contain brace characters.
    import re
    sanitized = re.sub(r'\"(?:\\.|[^\"\\])*\"', '""', s)
    sanitized = re.sub(r'//.*', '', sanitized)
    if sanitized.count('{') != sanitized.count('}'):
        checks.append((f'balanced braces: {p.relative_to(ROOT)}', False))

print(f'Kotlin files: {len(kt)}')
failed=0
for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + '  ' + name)
    failed += not ok
print('RESULT:', 'PASS' if failed == 0 else f'FAIL ({failed})')
raise SystemExit(1 if failed else 0)
