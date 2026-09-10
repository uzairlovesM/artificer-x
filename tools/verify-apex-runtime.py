#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/waheed/artificerx"
required = [
    "core/project/ProjectGraph.kt",
    "core/canvas/CanvasState.kt",
    "core/canvas/CanvasCoordinateMapper.kt",
    "core/canvas/GestureStateMachine.kt",
    "core/history/CanvasHistoryEngine.kt",
    "core/history/RecoveryJournal.kt",
    "core/render/RenderPlanEngine.kt",
    "core/render/RenderBudgetController.kt",
    "core/ai/runtime/CanvasOperationProtocol.kt",
    "core/ai/runtime/CanvasOperationGuard.kt",
    "core/ai/runtime/CanvasOperationApplier.kt",
    "core/runtime/CreativeRuntimeCoordinator.kt",
]
errors=[]
for rel in required:
    p=JAVA/rel
    if not p.exists(): errors.append(f"missing:{rel}")
    elif not p.read_text().strip(): errors.append(f"empty:{rel}")
checks = {
    "core/project/ProjectGraph.kt": ["fingerprint", "updateLayer", "addLayer", "removeLayer"],
    "core/canvas/CanvasCoordinateMapper.kt": ["screenToCanvas", "canvasToScreen", "zoomAroundScreenPoint"],
    "core/history/CanvasHistoryEngine.kt": ["record", "undo", "redo", "No-op revision"],
    "core/history/RecoveryJournal.kt": ["BEGIN", "VERIFY", "COMMIT", "ROLLBACK"],
    "core/render/RenderPlanEngine.kt": ["dirty", "tiles", "fullFrame"],
    "core/ai/runtime/CanvasOperationGuard.kt": ["stale_revision", "operation_batch_too_large", "selection_required"],
    "core/ai/runtime/CanvasOperationApplier.kt": ["deferredPixelOperations", "SetLayerOpacity", "DrawStroke"],
    "core/runtime/CreativeRuntimeCoordinator.kt": ["evaluateAiProposal", "commitCanvasFingerprint", "rollback"],
}
for rel,tokens in checks.items():
    p=JAVA/rel
    if not p.exists(): continue
    text=p.read_text()
    for token in tokens:
        if token not in text: errors.append(f"missing-logic:{rel}:{token}")

if errors:
    print("APEX RUNTIME VERIFICATION FAILED")
    print("\n".join(f"- {e}" for e in errors))
    sys.exit(1)
print("APEX RUNTIME VERIFICATION PASSED")
print("Stages 1-6 runtime backbone: present")
print(f"Kotlin source files: {sum(1 for _ in JAVA.rglob('*.kt'))}")
