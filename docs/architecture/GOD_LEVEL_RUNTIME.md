# ARTIFICER-X Runtime Architecture

ARTIFICER-X is organized as a local-first Android creative workspace with one persistent workspace database and an agent execution spine.

## Runtime contract

`AgentOrchestrator` receives a user turn, ranks enabled AI providers, compiles bounded conversation context, injects specialist/world/memory context, sends the full tool registry, executes returned tools, feeds tool results back into the model, and stops only on `finish_turn` or a hard iteration ceiling.

## Persistent workspace

The workspace database stores chat threads, messages, artifacts, and memories. The active thread ID is persisted independently so an app restart resumes the same conversation instead of silently switching to a new session.

## Artifact protocol

All generated files are written under the app-owned artifact store. ZIP packages include `ARTIFACT-MANIFEST.json` with MIME type, size and SHA-256 metadata for every packaged input.

## Tool model

Concrete tool schemas are the authoritative executable surface. The dynamic catalog provides discoverable capability aliases and is intentionally routed through a small safe adapter surface. A catalog entry is never treated as proof that an unsupported side effect exists.

## Feature consistency

The capability graph defines expected plugin families and concrete tools per feature. Diagnostics therefore checks dependency coverage rather than simply displaying the number of installed plugins.

## Security boundaries

Shell operations are filtered before execution, timeouts are clamped, artifact paths are app-owned, and distributable source archives do not contain local signing credentials or API-key material.
