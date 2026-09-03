#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
dest = (ROOT / "app/src/main/java/com/waheed/artificerx/ui/navigation/Destinations.kt").read_text()
nav = (ROOT / "app/src/main/java/com/waheed/artificerx/ui/navigation/ArtificerXNavGraph.kt").read_text()
routes = re.findall(r'const val ([A-Z][A-Z0-9_]+)\s*=\s*"([^"]+)"', dest)
missing = [(name, value) for name, value in routes if not name.endswith("_BASE") and name not in {"CANVAS", "PROJECT_DETAIL", "PROJECT_VERSION_HISTORY", "EXPORT", "COMPARISON_VIEW"} and f"Destinations.{name}" not in nav]
print(f"declared routes: {len(routes)}")
print(f"unreferenced constants: {len(missing)}")
for name, value in missing:
    print(f"- {name} = {value}")
raise SystemExit(1 if missing else 0)
