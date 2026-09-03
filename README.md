# ARTIFICER-X

Personal, private, agentic multi-tool AI art studio for Android.
Vision-reasoning brain (Groq/OpenRouter or other configured compatible providers) drives
local canvas/vector/procedural tools, persistent workspace memory, artifacts, and
provider-backed image generation when an image-capable provider is configured. Built solo, zero-budget, Termux + GitHub Actions.

## ARTIFICER-X Workspace Expansion

This revision adds a separate persistent workspace database for chat threads, message history and artifact metadata; real app-private artifact storage with ZIP creation; a terminal sandbox; a plugin manager and built-in capability catalog; and a provider-backed `generate_image` agent tool that emits a real PNG artifact and FileProvider URI. The tool registry also exposes a 1,000+ capability inventory while concrete high-impact operations remain explicitly implemented and safety-bounded.

New plugin domains include AI providers, models, image generation, code, files, export, canvas, 3D/sculpt, productivity, web, agent runtime, themes/UI, import/export, database, media, automation, security, networking and documents.
