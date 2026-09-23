#!/usr/bin/env python3
from __future__ import annotations
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app' / 'src' / 'main' / 'java'
errors = []
warnings = []

files = list(JAVA.rglob('*.kt'))
text = {p: p.read_text(encoding='utf-8', errors='replace') for p in files}

# Duplicate top-level fully-qualified declarations are a frequent source of KSP/compile drift.
# Only declarations at brace depth 0 are considered; nested Snapshot/Evaluation
# data classes inside generated capabilities are intentionally allowed.
decls = {}
for p, s in text.items():
    pkg = re.search(r'^\s*package\s+([\w.]+)', s, re.M)
    package = pkg.group(1) if pkg else ''
    depth = 0
    for line in s.splitlines():
        stripped = line.strip()
        if depth == 0:
            m = re.match(r'^(?:data\s+|sealed\s+|open\s+|abstract\s+|enum\s+)?(?:class|interface|object)\s+(\w+)', stripped)
            if m:
                fq = f'{package}.{m.group(1)}'
                decls.setdefault(fq, []).append(str(p))
        # Cheap lexical depth accounting. Generated project source is regular Kotlin;
        # strings/comments can contain braces, so this is a detector, not a parser.
        depth += line.count('{') - line.count('}')
        depth = max(depth, 0)
for fq, paths in decls.items():
    if len(paths) > 1:
        errors.append(f'duplicate top-level declaration {fq}: {paths}')

all_text = '\n'.join(text.values())

# Legacy or known-broken symbols from previous CI failures.
for bad in ('ApplicationComponent', 'scheduleDaily(', 'readAndroidPressureSensors', 'error.NonExistentClass'):
    if bad in all_text:
        errors.append(f'forbidden legacy/broken symbol remains: {bad}')

# Known Kotlin/Android API mistakes.
if re.search(r'writeText\([^\n]*,\s*(?:true|false)\s*\)', all_text):
    errors.append('File.writeText(value, appendBoolean) style call remains; use appendText/writeText with Charset instead')
if 'Regex("\\s' in all_text:
    errors.append('invalid Kotlin Regex escape remains')

# Every source reference to callback contract must resolve to the canonical interface.
callback_users = [p for p, s in text.items() if re.search(r'\bCallback\s*<', s)]
callback_file = JAVA / 'com' / 'waheed' / 'artificerx' / 'core' / 'runtime' / 'Callback.kt'
if callback_users and not callback_file.exists():
    errors.append('Callback<T> users exist but canonical core/runtime/Callback.kt is missing')

# Ensure the app entry point initializes the application context bridge used by real saves.
app = JAVA / 'com' / 'waheed' / 'artificerx' / 'ArtificerXApp.kt'
ctx = JAVA / 'com' / 'waheed' / 'artificerx' / 'util' / 'AppContextHolder.kt'
if 'AppContextHolder.init' not in text.get(app, ''):
    errors.append('ArtificerXApp does not initialize AppContextHolder')
if not ctx.exists():
    errors.append('AppContextHolder.kt missing')

# Chat turn reliability contracts.
orch = text.get(JAVA / 'com' / 'waheed' / 'artificerx' / 'core' / 'agent' / 'AgentOrchestrator.kt', '')
for required in ('safeMode = false', 'safeMode = true', 'providerSupportsReasoningEffort', 'ToolSelectionPolicy.select'):
    if required not in orch:
        errors.append(f'AgentOrchestrator reliability contract missing: {required}')

policy = text.get(JAVA / 'com' / 'waheed' / 'artificerx' / 'core' / 'agent' / 'ToolSelectionPolicy.kt', '')
if 'MAX_TOOLS = Int.MAX_VALUE' in policy:
    errors.append('tool catalog is unbounded per request')

# Lexical sanity checks. Ignore normal Kotlin strings, raw strings and comments
# so ordinary map/template braces do not create dozens of false-positive warnings.
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

pairs = {"}": "{", "]": "[", ")": "("}
for p, s in text.items():
    code_only = strip_non_code(s)
    stack = []
    mismatch = None
    for idx, ch in enumerate(code_only):
        if ch in "{[(":
            stack.append((ch, idx))
        elif ch in pairs:
            if not stack or stack[-1][0] != pairs[ch]:
                mismatch = (ch, idx)
                break
            stack.pop()
    if mismatch or stack:
        line = (code_only[:mismatch[1]].count("\n") + 1) if mismatch else code_only.count("\n") + 1
        warnings.append(f"unbalanced delimiter near line {line}: {p}")

print(f'SOURCE CONTRACT SCAN: {len(files)} Kotlin files')
for w in warnings[:40]:
    print(f'WARNING: {w}')
if len(warnings) > 40:
    print(f'WARNING: {len(warnings)-40} additional nested-brace candidates omitted')
if errors:
    print(f'SOURCE CONTRACTS FAILED: {len(errors)} error(s)')
    for e in errors:
        print(f'ERROR: {e}')
    raise SystemExit(1)
print('SOURCE CONTRACTS PASSED')
