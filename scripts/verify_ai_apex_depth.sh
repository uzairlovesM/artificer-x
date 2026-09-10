#!/usr/bin/env bash
set -euo pipefail
ROOT="app/src/main/java/com/waheed/artificerx/core/ai/apex"
[[ -d "$ROOT" ]]
FILES=$(find "$ROOT" -type f -name '*.kt' | wc -l)
LINES=$(find "$ROOT" -type f -name '*.kt' -print0 | xargs -0 cat | wc -l)
(( FILES >= 180 ))
(( LINES >= 50000 ))
grep -q 'class UniversalModelProtocol' "$ROOT/compat/UniversalModelProtocol.kt"
grep -q 'class UniversalAiRuntimeKernel' "$ROOT/deep/integration/UltimateAiRuntimeKernel.kt"
grep -q 'fun ChatCompletionRequest.normalizedForProvider' app/src/main/java/com/waheed/artificerx/core/network/ChatCompletionModels.kt
grep -q 'fun AiProviderConfig.effectiveEndpoint' app/src/main/java/com/waheed/artificerx/domain/model/AiProvider.kt
if grep -RIlE 'TODO|FIXME|NotImplementedException|UnsupportedOperationException' "$ROOT" >/dev/null; then
  echo "AI_APEX_UNFINISHED_MARKER_FOUND"
  exit 1
fi
echo "AI_APEX_DEPTH_GATE_PASS files=$FILES lines=$LINES"
