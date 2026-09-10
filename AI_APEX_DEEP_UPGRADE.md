# ArtificerX AI Apex Deep Upgrade

This upgrade expands the AI runtime with executable model compatibility, context management, routing, vision analysis, planning, agents, tools, memory, workflows, output verification, generation adapters, transport resilience, canvas intelligence, reasoning, and quality systems.

The AI deep layer is intentionally provider-neutral. A model is selected by declared capabilities rather than a hard-coded provider name. Supported protocol families include OpenAI Chat Completions, OpenAI Responses-style payloads, Gemini generateContent, Anthropic Messages, local llama.cpp-compatible chat, and generic JSON adapters.

No claim is made that every model is automatically compatible. Actual model support still depends on the provider endpoint, authentication, chat template, context window, modalities, tool schema, and model-specific behavior. The compatibility layer detects declared requirements, normalizes requests, assembles streamed tool calls, and exposes degradation paths.

The new runtime also separates proposed AI operations from application state mutation. Canvas-changing actions should continue to pass through the project transaction/recovery layer.
