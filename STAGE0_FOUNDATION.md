# ArtificerX 1.0 Stage 0 Foundation

Stage 0 establishes the executable boundaries that later canvas, drawing, AI and agent features must use.

## Canonical rules

- Project mutations go through `ProjectTransactionEngine`.
- Mutations use validate -> mutate -> validate -> commit semantics.
- A successful mutation advances the project revision exactly once and marks the project dirty.
- Failed mutations do not replace the authoritative snapshot.
- Operation execution has explicit validation, execution, verification and commit states.
- The mutation journal records operation lifecycle transitions for diagnostics and future persistent recovery.
- Boundary rules are explicit and can be checked by `FoundationHealthGate`.
- Capability descriptors are not considered implementations by themselves.

## Exit criteria

Stage 0 is complete only when the project can preserve a canonical project snapshot, execute an atomic mutation, reject invalid state without replacing the authoritative snapshot, report boundary violations, and compile the new foundation tests.

## What changed in alpha01

- Added canonical operation lifecycle tracking with execution/correlation identifiers.
- Added bounded mutation journaling for later persistent recovery.
- Added explicit architecture boundary contracts and a boundary health gate.
- Added an atomic project transaction engine with revision/checksum semantics.
- Existing Room/KSP repair remains untouched by this foundation layer.
