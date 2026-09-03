#!/usr/bin/env python3
"""Fast static integrity audit for ARTIFICER-X. No network required."""
from __future__ import annotations
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"

REQUIRED_FILES = [
    "core/agent/AgentOrchestrator.kt",
    "core/agent/ToolRegistry.kt",
    "core/agent/ToolCallParser.kt",
    "core/artifact/ArtifactStore.kt",
    "core/image/ImageGenerationService.kt",
    "core/plugin/PluginManager.kt",
    "data/workspace/WorkspaceDatabase.kt",
    "ui/screens/chat/AgentChatViewModel.kt",
]
REQUIRED_ROUTES = [
    "PLUGIN_CENTER", "DIAGNOSTICS", "TOOL_UNIVERSE", "ARTIFACT_HUB",
    "COMMAND_CENTER", "MEMORY_CENTER", "WORKFLOW_LAB", "MODEL_PLAYGROUND",
    "SECURITY_CENTER", "WORKSPACE_SEARCH",
]

errors: list[str] = []
warnings: list[str] = []

def fail(msg: str) -> None:
    errors.append(msg)

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

for rel in REQUIRED_FILES:
    if not (SRC / "com/waheed/artificerx" / rel).exists():
        fail(f"missing required source: {rel}")

try:
    ET.parse(MANIFEST)
except Exception as exc:
    fail(f"AndroidManifest XML invalid: {exc}")

registry = read("app/src/main/java/com/waheed/artificerx/core/agent/ToolRegistry.kt")
tool_names = re.findall(r'private fun (\w+)Tool\(\)', registry) + re.findall(r'name\s*=\s*"([a-zA-Z0-9_]+)"', registry)
# Only concrete registry function/schema names are counted from quoted names.
quoted_tools = set(re.findall(r'name\s*=\s*"([a-zA-Z0-9_]+)"', registry))
dynamic = read("app/src/main/java/com/waheed/artificerx/core/agent/DynamicToolCatalog.kt")
repeat = re.search(r'repeat\((\d+)\)', dynamic)
expected_dynamic = int(repeat.group(1)) * 20 if repeat else 0
runtime_tool_lower_bound = len(quoted_tools) + expected_dynamic
if runtime_tool_lower_bound < 1000:
    fail(f"tool surface unexpectedly small: lower bound={runtime_tool_lower_bound}")
if len(quoted_tools) != len(set(quoted_tools)):
    fail("duplicate concrete tool schema names detected")

plugins = read("app/src/main/java/com/waheed/artificerx/core/plugin/BuiltinPluginCatalog.kt")
literal_plugin_count = len(re.findall(r'add\(descriptor\(', plugins))
if "addAll(expansion())" not in plugins:
    fail("plugin expansion contract is not wired into BuiltinPluginCatalog")
if literal_plugin_count < 150:
    warnings.append(f"only {literal_plugin_count} literal plugin descriptors; expansion is expected to fill the rest")

# Route declarations vs navigation usage.
dest = read("app/src/main/java/com/waheed/artificerx/ui/navigation/Destinations.kt")
nav = read("app/src/main/java/com/waheed/artificerx/ui/navigation/ArtificerXNavGraph.kt")
for route in REQUIRED_ROUTES:
    if f"const val {route} " not in dest:
        fail(f"missing route declaration: {route}")
    if f"Destinations.{route}" not in nav:
        fail(f"route declared but not referenced by NavGraph: {route}")

# Secret material should never be packaged.
for forbidden in ["keystore_base64.txt", "*.jks", "*.keystore"]:
    matches = list(ROOT.rglob(forbidden)) if "*" in forbidden else [ROOT / forbidden]
    for item in matches:
        if item.is_file() and ".git" not in item.parts and "build" not in item.parts:
            fail(f"secret/signing material inside source tree: {item.relative_to(ROOT)}")

# Common broken states / merge debris.
for path in ROOT.rglob("*.kt"):
    if ".git" in path.parts or "build" in path.parts:
        continue
    text = path.read_text(encoding="utf-8", errors="ignore")
    if "<<<<<<<" in text or ">>>>>>>" in text or "=======" in text:
        fail(f"merge-conflict markers in {path.relative_to(ROOT)}")
    if "PlaceholderScreen" in text and path.name != "HybridFeatureScreen.kt":
        warnings.append(f"placeholder reference remains in {path.relative_to(ROOT)}")

summary = {
    "status": "PASS" if not errors else "FAIL",
    "errors": errors,
    "warnings": warnings,
    "literalPluginDescriptors": literal_plugin_count,
    "dynamicToolLowerBound": expected_dynamic,
    "concreteToolSchemaNames": len(quoted_tools),
    "runtimeToolLowerBound": runtime_tool_lower_bound,
}
print(json.dumps(summary, indent=2))
sys.exit(1 if errors else 0)
