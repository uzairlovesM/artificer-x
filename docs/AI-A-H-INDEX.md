# AI A-H implementation index

The A-H runtime is implemented under `app/src/main/java/com/waheed/artificerx/core/ai/apex` and is intentionally provider-neutral.

A. Agent OS: kernel, task graph, scheduler, reasoning graph, context, resource governor, event stream.
B. Canvas Intelligence: revisioned world model and structured observation hooks.
C. Visual Critic: pixel features, deterministic visual diff, critic reports, repair planning.
D. Multi-Agent Studio: specialist proposals, dependency-aware arbitration, parallel scheduling.
E. Creative Memory: scoped memories, retrieval, reinforcement, decay and eviction.
F. Generation Gateway: generation request planning, adapter contracts, artifact validation and deterministic offline test adapter.
G. Autonomous Workflow: dependency execution, retries, checkpoints and compensation.
H. Hybrid Runtime: model routing, health bookkeeping, permission policy, route explanations and the unified `ApexAiRuntime` facade.

The runtime intentionally does not claim that a deterministic fallback image is equivalent to a learned image-generation model. Real local/remote generators are connected through `GenerationAdapter` implementations.
