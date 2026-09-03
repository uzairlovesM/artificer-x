# Feature Consistency Rules

The project treats every user-visible capability as a contract graph.

`Screen -> ViewModel -> Repository/Service -> Persistence/Execution -> Output`

A feature is not considered healthy when any required edge is missing. Examples:

- AI Chat requires provider routing, model capability checks, memory, tool registry, persistence, and output delivery.
- Image generation requires provider credentials, image endpoint support, binary decoding, artifact storage, preview/share delivery, and failure handling.
- ZIP export requires file inputs, safe path normalization, archive creation, metadata, and integrity verification.
- Terminal automation requires a sandbox root, shell policy, timeout, output capture, and non-zero exit reporting.
- Plugin installation requires a catalog entry, persisted state, and dependency closure.
- Navigation requires both a route declaration and an actual composable entry.
