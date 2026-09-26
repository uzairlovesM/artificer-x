#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
root_text = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
root_aliases = set(re.findall(r"alias\(libs\.plugins\.([A-Za-z0-9_.-]+)\)", root_text))

missing = {}
for path in sorted(ROOT.glob("*/build.gradle.kts")):
    text = path.read_text(encoding="utf-8")
    aliases = set(re.findall(r"alias\(libs\.plugins\.([A-Za-z0-9_.-]+)\)", text))
    gap = sorted(aliases - root_aliases)
    if gap:
        missing[str(path.relative_to(ROOT))] = gap

if missing:
    print("::error::Subproject plugins missing from the root plugin classpath:")
    for path, aliases in missing.items():
        print(f"  {path}: {', '.join(aliases)}")
    sys.exit(1)

# Android test modules intentionally use the unversioned plugin id because
# com.android.test is registered once at the root with apply false. This
# prevents Gradle from seeing a second version request after another plugin
# has placed AGP classes on the classpath.
for rel in ("baselineprofile/build.gradle.kts", "benchmark/build.gradle.kts"):
    text = (ROOT / rel).read_text(encoding="utf-8")
    if 'id("com.android.test")' not in text:
        print(f"::error::{rel} must apply the root-registered com.android.test plugin")
        sys.exit(1)
    if 'alias(libs.plugins.android.test)' in text:
        print(f"::error::{rel} must not issue a second versioned com.android.test request")
        sys.exit(1)

print("Plugin classpath contract: PASS")
print(f"Root plugin aliases registered: {len(root_aliases)}")
print("All subproject plugin aliases are root-registered; Android test modules inherit the root AGP version.")
