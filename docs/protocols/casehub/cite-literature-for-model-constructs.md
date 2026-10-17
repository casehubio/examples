---
id: PP-20261017-d5853d
title: "Cite published literature for every model-derived code construct"
type: rule
scope: platform
applies_to: "All repos in the cognitive architecture (examples, neocortex, eidos, blocks) — any constant, threshold, algorithm, equation, or architectural pattern derived from a published psychological, cognitive, or computational model"
severity: important
refs: []
violation_hint: "A numeric constant, scoring function, or architectural pattern with no comment explaining which published model it implements"
created: 2026-10-17
---

Every model-derived constant, algorithm, threshold, equation, and architectural decision
must have a comment citing the published literature it's based on. No arbitrary numbers.
Format: `// Model name (Author Year) — what this grounds`. For classes implementing a
complete model, use Javadoc `@see Author, A. (Year). Title. Journal, Vol, Pages.`
The citation must be specific enough that a reader can find the paper and verify the
mapping. "Based on CAPS" is insufficient; "CAPS resting activation (Mischel & Shoda 1995,
Psychological Review, 102, 246-268) — self-reinforcing unit activation" is sufficient.
