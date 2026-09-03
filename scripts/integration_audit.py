#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/com/waheed/artificerx'
registry = (SRC / 'core/agent/ToolRegistry.kt').read_text()
parser = (SRC / 'core/agent/ToolCallParser.kt').read_text()
result = (SRC / 'core/agent/ToolExecutionResult.kt').read_text()
executor = (SRC / 'core/agent/ToolExecutor.kt').read_text()
orchestrator = (SRC / 'core/agent/AgentOrchestrator.kt').read_text()
issues = []

tool_names = re.findall(r'name\s*=\s*"([a-zA-Z0-9_]+)"', registry)
tool_names = list(dict.fromkeys(tool_names))
parser_names = set(re.findall(r'"([a-zA-Z0-9_]+)"\s*->\s*ParsedToolCall\.', parser))
for name in tool_names:
    if name not in parser_names:
        issues.append(f'tool missing parser case: {name}')
required = ['generate_image','create_file','create_zip','run_terminal_command','run_terminal_batch','remember','recall','search_workspace','export_workspace_bundle']
for name in required:
    if name not in tool_names: issues.append(f'missing registry tool: {name}')
    if name not in parser_names: issues.append(f'missing parser route: {name}')
for needle, label, hay in [
    ('GenerateImage', 'image execution branch', executor),
    ('CreateFile', 'file execution branch', executor),
    ('CreateZip', 'zip execution branch', executor),
    ('RunTerminalCommand', 'terminal execution branch', executor),
    ('ExportWorkspaceBundle', 'workspace export execution branch', executor),
]:
    if needle not in hay: issues.append(f'missing {label}')
if 'ToolSelectionPolicy.select(userText)' not in orchestrator:
    issues.append('provider requests bypass ToolSelectionPolicy')
for f in ROOT.rglob('*'):
    if f.is_file() and f.suffix in {'.kt','.kts','.xml','.json','.properties'}:
        text = f.read_text(errors='ignore')
        if '-----BEGIN PRIVATE KEY-----' in text or re.search(r'(?i)(api[_-]?key|password)\s*=\s*[^$\n]{20,}', text):
            if 'resolveSecret' not in text and 'password' in text.lower():
                issues.append(f'possible embedded secret: {f.relative_to(ROOT)}')
print('integration audit:', 'PASS' if not issues else 'FAIL')
print(f'concrete registry definitions: {len(tool_names)}')
if issues:
    for issue in issues: print('-', issue)
    sys.exit(1)
