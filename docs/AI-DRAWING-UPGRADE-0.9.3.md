# ArtificerX AI Drawing and Autonomy Upgrade 0.9.3

## Scope
This upgrade makes the drawing agent a closed-loop system rather than a one-shot drawing command. The intended loop is research, semantic scene compilation, structured rendering, visual inspection, defect classification, repair planning, and final artifact verification.

## Visual intelligence
The project now contains explicit VisionFrame, VisionObservation, VisualResearchLoop, ReferenceResearchPlan, and VisionInspector contracts. The inspector currently provides deterministic pixel-level measurements and a defect vocabulary. Provider-specific object detection can be connected through the same observation contract without changing the drawing planner.

## Drawing intelligence
DrawingIntent and SceneSpec separate the user's request from executable scene structure. SceneCompiler creates architectural and furniture nodes, camera parameters, palette information, and semantic labels. DrawingAgentLoop and DrawingQualityGate provide the quality feedback boundary needed for iterative correction.

## Tool autonomy
CapabilityBroker and AutonomyController expose registered runtime capabilities without imposing an arbitrary per-message tool-count quota. Runtime extensions remain separately validated. The application does not bypass Android security boundaries or provider-imposed limits; it can only control capabilities actually granted to the process and configured providers.

## Creative controls
BrushDefinition, BrushDynamics, PerspectiveGrid, LayerGraph, and RepairStrategy provide explicit contracts for brush dynamics, pressure, velocity, geometry, layer organization, and automated repair.

## Research
ResearchLoop and DeepResearchCoordinator provide ranking and contradiction detection. The agent can use web references as evidence for visual planning rather than blindly guessing the appearance of an unfamiliar environment.

## Verification
The current source tree was checked for Kotlin brace balance and empty directories. Full Android Gradle compilation is not claimed unless the required Gradle distribution and dependencies are available in the execution environment.
