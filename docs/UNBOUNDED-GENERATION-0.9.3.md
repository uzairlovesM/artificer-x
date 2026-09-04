# ArtificerX 0.9.3 — Unbounded Application Generation Pass

- Removed the application-side 2K/4K-style output ceiling by keeping `max_tokens` unset.
- Removed the 180-tool request ceiling.
- Removed the context compiler character ceiling for normal agent turns.
- Added continuation turns when a provider reports an output-length finish reason.
- Removed the local generation wall-clock timeout; cancellation/user stop remains authoritative.
- Increased the default imported GGUF context target to 32768 tokens.

These changes mean ArtificerX no longer imposes a small artificial output cap. Actual provider/model context and output limits, Android memory, network/server limits, and provider pricing/rate limits remain external constraints that the application cannot remove.
