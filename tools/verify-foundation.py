#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/waheed/artificerx"
failures = []

required = [
    JAVA / "core/foundation/ExecutionId.kt",
    JAVA / "core/foundation/OperationState.kt",
    JAVA / "core/foundation/MutationJournal.kt",
    JAVA / "core/architecture/ArchitectureContracts.kt",
    JAVA / "core/architecture/FoundationHealthGate.kt",
    JAVA / "core/operation/Operation.kt",
    JAVA / "core/project/ProjectTransactionEngine.kt",
    JAVA / "core/project/ProjectGraph.kt",
    JAVA / "core/canvas/CanvasState.kt",
    JAVA / "core/canvas/CanvasCoordinateMapper.kt",
    JAVA / "core/canvas/GestureStateMachine.kt",
    JAVA / "core/history/CanvasHistoryEngine.kt",
    JAVA / "core/history/RecoveryJournal.kt",
    JAVA / "core/render/RenderPlanEngine.kt",
    JAVA / "core/render/RenderBudgetController.kt",
    JAVA / "core/ai/runtime/CanvasOperationProtocol.kt",
    JAVA / "core/ai/runtime/CanvasOperationGuard.kt",
    JAVA / "core/ai/runtime/CanvasOperationApplier.kt",
    JAVA / "core/runtime/CreativeRuntimeCoordinator.kt",
]
for path in required:
    if not path.exists():
        failures.append(f"missing foundation file: {path.relative_to(ROOT)}")

operation = (JAVA / "core/operation/Operation.kt").read_text()
for token in ["VALIDATING", "RUNNING", "VERIFYING", "COMMITTED", "FAILED", "ROLLED_BACK", "operation.validated", "operation.verifying"]:
    if token not in operation:
        failures.append(f"operation lifecycle missing: {token}")

project = (JAVA / "core/project/ProjectTransactionEngine.kt").read_text()
for token in ["validator.validate", "revision = before.metadata.revision + 1", "dirty = true", "sha256"]:
    if token not in project:
        failures.append(f"project transaction invariant missing: {token}")

build = (ROOT / "app/build.gradle.kts").read_text()
m = re.search(r"versionCode\s*=\s*(\d+).*?versionName\s*=\s*\"([^\"]+)\"", build, re.S)
if not m or m.group(1) != "12" or m.group(2) != "1.0.0-alpha03":
    failures.append("version was not advanced to 1.0.0-alpha03 / 12")

for p in ROOT.rglob("*.kt"):
    data = p.read_bytes()
    if b"\x00" in data:
        failures.append(f"NUL byte found: {p.relative_to(ROOT)}")
        break

if failures:
    print("FOUNDATION VERIFICATION FAILED")
    for f in failures:
        print(f"- {f}")
    sys.exit(1)

print("FOUNDATION VERIFICATION PASSED")
print("Stage: 0-6 / Architecture Foundation + Runtime Backbone")
print("Version: 1.0.0-alpha03")
print("Canonical transaction engine: present")
print("Operation lifecycle journal: present")
print("Architecture boundary gate: present")
