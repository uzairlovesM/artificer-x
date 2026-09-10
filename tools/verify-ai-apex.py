#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'app/src/main/java/com/waheed/artificerx/core/ai/apex'
required = [
    'ApexAiRuntime.kt',
    'context/ContextEngine.kt',
    'kernel/ApexRuntime.kt',
    'kernel/ApexAgentKernel.kt',
    'agent/AgentTaskGraph.kt',
    'agent/MultiAgentSupervisor.kt',
    'agent/AgentScheduler.kt',
    'model/ModelRouter.kt',
    'memory/CreativeMemoryStore.kt',
    'vision/CanvasWorldModel.kt',
    'vision/VisualDiffEngine.kt',
    'vision/VisualCritic.kt',
    'vision/PixelFeatureExtractor.kt',
    'vision/RepairPlan.kt',
    'reasoning/ReasoningGraph.kt',
    'reasoning/ReasoningPolicy.kt',
    'reasoning/PromptCompiler.kt',
    'tool/ToolRuntime.kt',
    'tool/ToolAuthorization.kt',
    'tool/ToolRetryPolicy.kt',
    'tool/ToolResultLedger.kt',
    'generation/GenerationGateway.kt',
    'generation/ArtifactValidator.kt',
    'generation/GenerationRequestPlanner.kt',
    'workflow/AutonomousWorkflowEngine.kt',
    'workflow/WorkflowCompensation.kt',
    'workflow/WorkflowCheckpoint.kt',
    'hybrid/HybridAiRuntime.kt',
    'hybrid/ProviderHealthBook.kt',
    'permission/AiPermissionPolicy.kt',
]
errors=[]
for rel in required:
    p=BASE/rel
    if not p.exists() or not p.read_text().strip(): errors.append('missing-or-empty:'+rel)
count=sum(1 for _ in BASE.rglob('*.kt'))
if count < 40: errors.append(f'apex-count-too-small:{count}')
checks={
 'ApexAiRuntime.kt':['analyze','route','critique','generate','verify'],
 'kernel/ApexAgentKernel.kt':['plan','diagnostics'],
 'agent/AgentScheduler.kt':['parallelWaves','cycles'],
 'vision/VisualDiffEngine.kt':['changedPixels','meanAbsoluteError'],
 'vision/VisualCritic.kt':['NO_VISIBLE_CHANGE','CHANGE_SCOPE'],
 'memory/CreativeMemoryStore.kt':['remember','recall','decay','forget'],
 'generation/GenerationGateway.kt':['IMAGE_TO_IMAGE','SKETCH_TO_IMAGE','generate'],
 'workflow/AutonomousWorkflowEngine.kt':['ready','completed','failed'],
 'tool/ToolAuthorization.kt':['confirmation_required','missing_permission'],
 'reasoning/ReasoningGraph.kt':['OBSERVING','PLANNING','EXECUTING','VERIFYING','REPAIRING'],
}
for rel,tokens in checks.items():
    text=(BASE/rel).read_text()
    for token in tokens:
        if token not in text: errors.append(f'missing-logic:{rel}:{token}')
if errors:
    print('AI APEX VERIFICATION FAILED')
    print('\n'.join(errors)); sys.exit(1)
print('AI APEX VERIFICATION PASSED')
print(f'A-H runtime files: {count}')
