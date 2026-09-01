# ArtificerX — Feature Gap Research

Researched via Exa (web_search_exa) on 2026-09-02. Compares ArtificerX's
current capabilities against (A) professional mobile painting apps and
(B) AI-agent-driven drawing/canvas systems, since ArtificerX is
architecturally closer to category B (an LLM drives real canvas tool
calls) than a traditional diffusion-based AI art app.

This is a planning document, not a changelog — it records what exists
elsewhere so future work can be scoped against real prior art instead
of guesswork. See `CanvasModels.kt`, `CanvasCompositor.kt`, and
`ToolExecutor.kt` for what ArtificerX currently has.

---

## A. Professional mobile painting apps — feature census

Sources: Infinite Painter (official docs + Play Store listing),
ibisPaint X (Play Store + changelog), Procreate (official handbook).

### Brush engine
| Feature | Infinite Painter | ibisPaint X | Procreate | ArtificerX now |
|---|---|---|---|---|
| Built-in brush presets | ~200 | 105,000+ (community) | ~200 curated | 0 (single hardcoded round brush) |
| Custom brush editor | ~100 tunable parameters | Pressure-sensitivity graphs per brush | Full brush studio | None |
| Pressure/tilt support | Yes, with calibration curves | Yes | Yes (Apple Pencil) | Not read from touch input at all |
| Brush blending modes | 25 | 27 | Full blend-mode list | Brush has no blend mode, only layer does |
| Paper/canvas texture interaction | Yes | Via materials library | Yes | None |
| Stroke stabilization / smoothing | — | Yes (explicit feature) | Yes (StreamLine) | None — raw point list, no smoothing |

**Gap severity: HIGH.** ArtificerX's entire "brush" is one hardcoded
`Paint.Style.STROKE` circle-cap line in `CanvasCompositor.drawPath`.
No texture, no pressure, no stabilization. This is the single biggest
gap versus every app researched.

### Layers
| Feature | Infinite Painter | ibisPaint X | ArtificerX now |
|---|---|---|---|
| Layer count | Up to thousands (canvas-size dependent) | Unlimited | Unlimited (Room-backed) |
| Blend modes | 25 | 27 | 10 (`LayerBlendMode` enum) — only 5 actually mapped to a `PorterDuffXfermode` in `blendModeToPorterDuff`; the rest silently fall through to `null` (Normal) |
| Layer groups | Yes (iOS), separate blend controls | — | None |
| Clipping masks | Yes | Yes | `createMask` exists but is a static shape mask, not a true "clip to layer below" |
| Adjustment/filter layers (non-destructive) | Yes (Gradient map, Color Curves, Filter layers) | Yes (15 filters as adjustment layers) | None — `applyFilter` is destructive, bakes into the bitmap immediately |
| Layer locking | Yes | Yes | Yes (`isLocked`, already wired) |

**Gap severity: MEDIUM-HIGH.** The blend-mode enum overpromises: 5 of
10 declared modes (`OVERLAY`, `COLOR_DODGE`, `COLOR_BURN`, `SUBTRACT`,
and effectively `NORMAL`'s siblings) have no real Porter-Duff
equivalent wired, so selecting them from UI would silently do nothing
different from Normal. This is a real, user-visible bug — worth fixing
before adding anything new.

### Tools
| Feature | Infinite Painter | ibisPaint X | ArtificerX now |
|---|---|---|---|
| Selection tools | Lasso, Magic Wand, Brush/quick-mask, Color Range, boolean ops | Vector lasso | `SELECTION` exists as an enum value in `DrawToolType` but **has no implementation anywhere** — not in `CanvasTouchOverlay`, not in `CanvasCompositor` |
| Transform (move/scale/rotate/skew/warp) | Full set, multi-layer | Yes | None |
| Perspective/isometric guides | 5 types | — | None |
| Symmetry | 4 types, up to 32-plane radial/kaleidoscope | Radial + symmetry rulers | 5 modes already implemented (`SymmetryMode`) — actually competitive here |
| Smudge/blend/liquify | Smudge brush, Liquify (move/bloat/pinch/swirl) | — | None |
| Gradient/pattern fills | Yes, with presets | Gradient pen | `applyGradient` + `applyPattern` exist and work |
| Text tool | Full editable text layers, fonts, RTL support | Vertical/horizontal, stroke, multi-font | `addText` exists but is a single non-editable `canvas.drawText` bake — no font choice, no re-editing after placement |
| Undo/redo | "Very fast multiple-step ... limited only by memory," slider scrubber | Standard | **Just implemented this session** — snapshot-based, 25-step cap |
| Timelapse recording | Yes, shareable | Yes (community feature) | None |

**Gap severity: HIGH** on Selection and Transform specifically — these
are core-workflow tools present in literally every competing app and
completely absent here, more so than "missing polish" items like
timelapse.

### Color & filters
| Feature | Infinite Painter | ibisPaint X | ArtificerX now |
|---|---|---|---|
| Color picker modes | RGB/HSB/Lab/CMYK/Hex | — | Hex only (`brushColorHex: String`) |
| Palettes | Multiple, dockable, unlimited swatches | Multiple, import/export | None |
| Filters | 40+ | 84 | 6 (`grayscale, invert, saturation, brightness, contrast, blur, sharpen`) — solid basics, missing the "creative" filter category entirely (chromatic aberration, glitch, pixelate, wave, polar coordinates, etc.) |
| Tone/color curves | Per-channel RGB curve tool | Tone Curve, Levels (premium) | None |

**Gap severity: MEDIUM.** The filter set is a reasonable *core*, just
shallow. Color picker being hex-only is a real UX gap, not a technical
one — cheap to fix (Compose has HSV conversion utilities already).

---

## B. AI-agent-driven canvas systems — feature census

This is the more relevant comparison class, since ArtificerX's
differentiator is "vision-reasoning LLM drives tool calls to draw,"
not diffusion generation. Four systems researched:

### 1. `genneth/monet` — iterative SVG art generator
- Render → look → iterate loop: model sees the canvas as an image
  after each pass and decides what to add next, up to N iterations.
- Keeps a running **"artist notes" scratchpad** across iterations —
  a persistent plan the model writes to and reads back, separate from
  the final artifact.
- Separates **planning** (one thinking-heavy pass up front: palette,
  composition, technique) from **execution** (many fast, non-thinking
  passes that just emit strokes).
- Ships an **artist statement** generation step at the end — a
  reflective/explanatory text artifact alongside the image.

**Relevant to ArtificerX:** `AgentOrchestrator`/`AgentEvent` already
has a vision-feedback loop (Section 156 per the codebase comments),
but there's no evidence of a persistent cross-turn scratchpad distinct
from chat history, and no explicit plan/execute phase split. Worth
checking `AgentOrchestrator.kt` for this in the next pass.

### 2. RefineSVG (arXiv paper + reference implementation)
- Introduces a **Diff-Map**: instead of just re-showing the model a
  flat rendered image, it computes a structured visual diff between
  target and current-render and feeds that back as a third image
  (target, current, diff) — a much stronger correction signal than
  "here's a picture, guess what's wrong."
- Explicitly frames the renderer as "the external ReAct environment"
  — the render step itself is a tool call, not a side effect.

**Relevant to ArtificerX:** `inspect_canvas` (per `ToolExecutor.kt`)
gives the agent a snapshot, but there's no diff/delta signal between
"what I intended" and "what's actually on the bitmap now." For a
reference-image-driven workflow (`import_image_layer` already exists),
adding an optional diff-overlay to `inspect_canvas`'s output would be
a direct, high-value port of this idea.

### 3. tldraw Agent Starter Kit — closest architectural sibling
This is the most structurally similar system to ArtificerX found:
- **Three-tier shape representation** sent to the model depending on
  relevance: `FocusedShape` (full detail, for shapes the agent is
  actively working on), `BlurryShape` (bounds + type only, for
  in-viewport-but-not-focused shapes), `PeripheralShapeCluster`
  (grouped counts only, for off-viewport awareness). This bounds
  prompt size on complex canvases without losing all spatial context.
- **Modes**: the agent can operate in different modes (e.g. a
  `working` mode with full drawing actions vs. a narrower `critique`
  mode with only review actions), switchable mid-task, each with its
  own allowed action set.
- **`sanitizeAction()` hook**: every agent action passes through a
  correction/validation step *before* being applied or saved to
  history — a structured place to catch and silently fix small
  LLM mistakes (out-of-bounds coordinates, invalid IDs) rather than
  hard-failing the tool call.
- **Streaming, incremental application**: shapes are created/updated
  as the model's response streams in, not only after the full
  response is parsed — the canvas visibly builds up turn-by-turn.
- **`schedule()` for multi-turn follow-up work** and **`interrupt()`**
  to redirect the agent mid-task with a new prompt/mode.

**Relevant to ArtificerX:** ArtificerX's `ParsedToolCall` sealed class
is architecturally similar to tldraw's action-util system, but:
  - No equivalent of "modes" — `ToolExecutor.executeSculptOnly()` vs
    `.execute()` is a coarse binary version of this idea (2D vs 3D),
    not a general mode system.
  - No sanitize/correction step — malformed tool-call args currently
    either get clamped defensively inline (e.g. `cropLayer`'s
    coordinate clamping) or presumably fail. A dedicated
    pre-validation hook (matching tldraw's `sanitizeAction()`) would
    centralize this instead of scattering `.coerceIn()` calls across
    every `CanvasCompositor` method.
  - No streaming/incremental application — needs confirming against
    `AgentOrchestrator.kt`, but the "flush once per turn" pattern in
    `StudioViewModel.recomposite()`'s debounce comment suggests tool
    calls apply immediately per-call already, which is actually fine;
    what's missing is showing partial *text* (the model's reasoning)
    as it streams, if that isn't already wired in `AgentChatViewModel`.

### 4. OpenClaw Live Canvas (product, not open-source reference)
- Model-agnostic rendering surface: works with any backend (Claude,
  GPT-4, DeepSeek, local via Ollama) — relevant precedent for
  ArtificerX's own multi-provider ambition (Groq/OpenRouter/local).
- Supports multiple *output formats* from one surface (HTML, SVG,
  Mermaid, Chart.js) and picks the format based on request type,
  rather than being single-format.

**Relevant to ArtificerX:** Confirms the local-model routing goal
(item #3/#4 in the user's original ask) is a legitimate, proven
pattern elsewhere, not unusual — worth using as a design reference
once `LocalInferenceEngine.kt`/`LocalLlamaAdapter.kt` get their deep
review.

---

## Prioritized gap list (highest real-world impact first)

1. **Blend-mode enum overpromise** (bug, not gap) — 5 of 10 declared
   `LayerBlendMode` values don't do anything different from Normal.
   Cheap fix, high user-trust impact since it *looks* like it works
   from the UI.
2. **Selection tool** — enum exists, zero implementation anywhere.
   Blocks an entire category of common workflows (move/erase/fill
   within a bound region).
3. **Brush realism** — pressure input is available from Android's
   `MotionEvent` (`getPressure()`) but nothing in `CanvasTouchOverlay`
   reads it; stroke width/opacity are currently flat per-stroke, not
   pressure-modulated. This is the difference every user *feels*
   immediately, even before comparing feature checklists.
4. **Transform tool** (move/scale/rotate/skew a selection or layer)
   — universally present elsewhere, absent here.
5. **Agent tool-call validation/sanitize step** — port tldraw's
   `sanitizeAction()` pattern into `ToolExecutor` as a pre-execution
   hook, centralizing the defensive-clamping logic that's currently
   duplicated ad hoc across `CanvasCompositor`.
6. **Visual diff feedback** (RefineSVG pattern) — extend
   `inspect_canvas` to optionally return a diff overlay against a
   reference image, not just a flat snapshot.
7. **Non-destructive adjustment layers** — every `applyFilter` call
   currently bakes permanently; competing apps treat filters as
   editable layers.
8. **Color picker upgrade** — hex-only input, no HSB/RGB sliders, no
   palettes.
9. **Stroke stabilization/smoothing** — raw point list with no
   smoothing pass; visible as "jittery" lines especially on touch
   (non-stylus) input.
10. **Creative filter set expansion** — current 6 filters are the
    correct *foundation* (grayscale/invert/saturation/brightness/
    contrast/blur/sharpen) but missing the "fun/distinct" category
    every competitor has (glitch, pixelate, wave, chromatic
    aberration).

---

## Notes on scope

This document intentionally does NOT propose implementation for all
10 items at once — see the earlier conversation for why doing that in
one pass produces worse code than doing it in an ordered sequence.
Item 1 (blend-mode bug) and item 5 (sanitize hook) are the best
next-session starting points: both are contained, testable in
isolation, and item 1 in particular is a correctness bug hiding behind
a UI that suggests it already works.
