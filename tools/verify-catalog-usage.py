#!/usr/bin/env python3
"""Verify Gradle version-catalog accessors and local plugin declarations."""
from __future__ import annotations
import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "gradle/libs.versions.toml"
cat = tomllib.loads(CATALOG.read_text(encoding="utf-8"))
errors: list[str] = []

def accessor_names(section: str) -> set[str]:
    out: set[str] = set()
    for key in cat.get(section, {}):
        # Gradle version-catalog aliases turn kebab-case segments into
        # dot-separated accessors, e.g. androidx-core-ktx -> libs.androidx.core.ktx.
        out.add(key.replace("-", "."))
    return out

library_aliases = accessor_names("libraries")
plugin_aliases = accessor_names("plugins")
bundle_aliases = accessor_names("bundles")
version_aliases = set(cat.get("versions", {}))

all_aliases = library_aliases | plugin_aliases | bundle_aliases

for path in sorted(ROOT.rglob("*.gradle.kts")):
    if "/build/" in str(path) or "/.gradle/" in str(path):
        continue
    raw_text = path.read_text(encoding="utf-8")
    # Catalog references in comments are documentation, not accessors.
    text = "\n".join(line.split("//", 1)[0] for line in raw_text.splitlines())
    for m in re.finditer(r"\blibs\.([A-Za-z0-9_.-]+)", text):
        token = m.group(1)
        if token.startswith("versions."):
            pieces = token.split(".")
            if len(pieces) < 2 or pieces[1] not in version_aliases:
                errors.append(f"{path}: unknown version accessor libs.versions.{'.'.join(pieces[1:])}")
            continue
        if token.startswith("plugins."):
            token = token[len("plugins."):]
            # plugin accessors may carry `.pluginId` or `.get()` suffixes in helper code.
            token = token.split(".pluginId", 1)[0]
            if token not in plugin_aliases:
                errors.append(f"{path}: unknown plugin catalog accessor libs.plugins.{token}")
            continue
        for prefix in ("bundles.",):
            if token.startswith(prefix):
                token = token[len(prefix):]
                candidates = bundle_aliases
                break
        else:
            candidates = all_aliases
        # Find the longest valid accessor prefix, allowing Gradle API suffixes like .get().toInt().
        matched = False
        for candidate in sorted(candidates, key=len, reverse=True):
            if token == candidate or token.startswith(candidate + "."):
                matched = True
                break
        if not matched:
            errors.append(f"{path}: unknown catalog accessor libs.{token}")

# Literal dependency/plugin versions are a second authority. Permit URLs and
# version strings inside comments, but reject actual declaration lines.
for path in sorted(ROOT.rglob("*.gradle.kts")):
    if "/build/" in str(path):
        continue
    for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        code = line.split("//", 1)[0]
        if re.search(r"\b(?:implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|ksp|kapt|detektPlugins)\s*\(\s*[\"'][^\"']+:[^\"']+:[0-9][^\"']*[\"']", code):
            errors.append(f"{path}:{lineno}: literal dependency version; use libs.versions.toml")
        if re.search(r"\bid\s*\(\s*[\"'][^\"']+[\"']\s*\)\s*version\s*[\"']", code):
            errors.append(f"{path}:{lineno}: literal plugin version; use the root plugin catalog")

if errors:
    print("::error::Catalog usage contract failed")
    for error in errors[:200]:
        print(error)
    if len(errors) > 200:
        print(f"... and {len(errors)-200} more")
    sys.exit(1)

print("CATALOG USAGE CONTRACT: PASS")
print(f"libraries={len(library_aliases)} plugins={len(plugin_aliases)} bundles={len(bundle_aliases)} versions={len(version_aliases)}")
