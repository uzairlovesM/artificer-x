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

# Lexical delimiter balance, useful for catching edit damage before a real build.
def strip_non_code(source: str) -> str:
    out = []
    i = 0
    state = "code"
    interpolation_depth = 0

    while i < len(source):
        if state == "code":
            if source.startswith("//", i):
                state = "line_comment"; out.append("  "); i += 2; continue
            if source.startswith("/*", i):
                state = "block_comment"; out.append("  "); i += 2; continue
            if source.startswith('\"\"\"', i):
                state = "raw_string"; out.append("   "); i += 3; continue
            if source[i] == '"':
                state = "string"; out.append(" "); i += 1; continue
            if source[i] == "'":
                state = "char"; out.append(" "); i += 1; continue
            out.append(source[i]); i += 1
        elif state == "line_comment":
            if source[i] == "\n": state = "code"; out.append("\n")
            else: out.append(" ")
            i += 1
        elif state == "block_comment":
            if source.startswith("*/", i): state = "code"; out.append("  "); i += 2
            else:
                out.append("\n" if source[i] == "\n" else " "); i += 1
        elif state == "raw_string":
            if source.startswith('\"\"\"', i): state = "code"; out.append("   "); i += 3
            else:
                out.append("\n" if source[i] == "\n" else " "); i += 1
        elif state == "string":
            if source[i] == "\\":
                out.extend([" ", " "]); i += 2
            elif source.startswith("${", i):
                out.extend([" ", " "]); i += 2; state = "interpolation"; interpolation_depth = 1
            elif source[i] == '"':
                state = "code"; out.append(" "); i += 1
            else:
                out.append("\n" if source[i] == "\n" else " "); i += 1
        elif state == "char":
            if source[i] == "\\":
                out.extend([" ", " "]); i += 2
            elif source[i] == "'":
                state = "code"; out.append(" "); i += 1
            else:
                out.append(" "); i += 1
        elif state == "interpolation":
            if source.startswith("//", i):
                state = "interpolation_line_comment"; out.append("  "); i += 2; continue
            if source.startswith("/*", i):
                state = "interpolation_block_comment"; out.append("  "); i += 2; continue
            if source.startswith('\"\"\"', i):
                state = "interpolation_raw_string"; out.append("   "); i += 3; continue
            if source[i] == '"':
                state = "interpolation_string"; out.append(" "); i += 1; continue
            if source[i] == "'":
                state = "interpolation_char"; out.append(" "); i += 1; continue
            if source[i] == "{":
                interpolation_depth += 1; out.append(" "); i += 1; continue
            if source[i] == "}":
                interpolation_depth -= 1; out.append(" "); i += 1
                if interpolation_depth == 0:
                    state = "string"
                continue
            out.append("\n" if source[i] == "\n" else " "); i += 1
        elif state == "interpolation_line_comment":
            if source[i] == "\n": state = "interpolation"; out.append("\n")
            else: out.append(" ")
            i += 1
        elif state == "interpolation_block_comment":
            if source.startswith("*/", i): state = "interpolation"; out.append("  "); i += 2
            else:
                out.append("\n" if source[i] == "\n" else " "); i += 1
        elif state == "interpolation_raw_string":
            if source.startswith('\"\"\"', i): state = "interpolation"; out.append("   "); i += 3
            else:
                out.append("\n" if source[i] == "\n" else " "); i += 1
        elif state == "interpolation_string":
            if source[i] == "\\":
                out.extend([" ", " "]); i += 2
            elif source[i] == '"':
                state = "interpolation"; out.append(" "); i += 1
            else:
                out.append(" "); i += 1
        elif state == "interpolation_char":
            if source[i] == "\\":
                out.extend([" ", " "]); i += 2
            elif source[i] == "'":
                state = "interpolation"; out.append(" "); i += 1
            else:
                out.append(" "); i += 1
    return ''.join(out)

pairs = {'}':'{', ']':'[', ')':'('}
for p in kt:
    code = strip_non_code(p.read_text(errors='ignore'))
    stack=[]; mismatch=False
    for ch in code:
        if ch in '{[(': stack.append(ch)
        elif ch in pairs:
            if not stack or stack[-1] != pairs[ch]: mismatch=True; break
            stack.pop()
    if mismatch or stack:
        checks.append((f'balanced delimiters: {p.relative_to(ROOT)}', False))

print(f'Kotlin files: {len(kt)}')
failed=0
for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + '  ' + name)
    failed += not ok
print('RESULT:', 'PASS' if failed == 0 else f'FAIL ({failed})')
raise SystemExit(1 if failed else 0)
