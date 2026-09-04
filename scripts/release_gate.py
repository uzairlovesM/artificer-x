#!/usr/bin/env python3
"""Deterministic source-release gate for Artificer-X.
This complements Gradle checks when the Android SDK/distribution is unavailable.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app" / "src" / "main" / "java"
errors = []
kt = list(SRC.rglob("*.kt"))
if len(kt) < 150:
    errors.append(f"Unexpectedly low Kotlin source count: {len(kt)}")
text = "\n".join(p.read_text(errors="ignore") for p in kt)
checks = {
    "persistent_chat": "ChatWorkspaceRepository",
    "persistent_memory": "MemoryRepository",
    "artifact_store": "ArtifactStore",
    "image_pipeline": "ImageGenerationService",
    "terminal_sandbox": "TerminalSandbox",
    "runtime_tools": "RuntimeToolCatalog",
    "plugin_catalog": "BuiltinPluginCatalog",
    "workspace_search": "WorkspaceSearch",
    "workspace_import": "WorkspaceBundleImporter",
    "workspace_export": "WorkspaceBundleService",
    "intent_router": "AgentIntentRouter",
    "tool_selection": "ToolSelectionPolicy",
    "security_redaction": "SecretRedaction",
}
for label, symbol in checks.items():
    if symbol not in text:
        errors.append(f"Missing required subsystem symbol: {label} ({symbol})")
if "PlaceholderScreen" in text:
    errors.append("PlaceholderScreen reference remains in production Kotlin")
for marker in ("TODO", "FIXME", "<<<<<<<", ">>>>>>>"):
    if marker in text:
        errors.append(f"Forbidden source marker remains: {marker}")
secret_names = ("keystore_base64.txt", "local.properties")
for name in secret_names:
    if (ROOT / name).exists():
        errors.append(f"Secret/local file exists in source root: {name}")
report = {
    "status": "PASS" if not errors else "FAIL",
    "kotlinSources": len(kt),
    "errors": errors,
}
print(report)
sys.exit(1 if errors else 0)
