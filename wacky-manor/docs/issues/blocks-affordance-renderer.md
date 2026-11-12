# Issue: casehubio/blocks — AffordanceRenderer

**Target repo:** casehubio/blocks
**Status:** Ready to file (gh auth down)

## Title

AffordanceRenderer — grounded observation rendering for LLM agents

## Body

### Context

Wacky Manor Phase 2.5 (autonomous character validation) revealed that LLM agents cannot reliably map narrative intent to structured actions when observations lack affordance grounding. The agent reasons correctly in natural language but generates wrong action types (INTERACT instead of TAKE) because:

1. **No object identity** — observation shows display names ("Rat Poison") but actions need IDs ("poison")
2. **No action affordance** — objects show properties but not which action type applies
3. **No consequence links** — no connection between inventory items and objects they can be used on

**Validated fix:** Adding the complete grounding chain to observations — `[id: poison] [TAKE to pick up] [USE with: rat-poison]` — enabled the LLM to autonomously TAKE poison and USE it on the tea service. Action descriptions alone (explaining what TAKE means) did NOT fix the problem.

### Requirement

Add an `AffordanceRenderer` to casehub-blocks that takes a structured model of observable entities with affordances and renders grounded observation text.

```java
record ObservableEntity(String id, String displayName, String description,
                         List<Affordance> affordances) {}

record Affordance(String actionType, String label,
                  String requiredItem, List<String> acceptsItems) {}

class AffordanceRenderer {
    String renderEntities(List<ObservableEntity> entities);
    String renderActionVocabulary(List<ActionDescriptor> actions);
}
```

**Application provides:** what entities are visible, what actions apply, what consequences exist
**AffordanceRenderer provides:** the rendering format that makes LLMs act correctly

### Integration path

Wacky Manor Phase 2.6 wires casehub-blocks' observation accumulator. The AffordanceRenderer would be part of that pipeline — replacing the hand-rolled observation code with the platform renderer.

### Evidence

- Phase 2.5 spec: `examples/wacky-manor/docs/specs/2026-07-27-phase-2.5-autonomous-characters-design.md`
- Validation commit: grounded affordances -> scenario ends POISONED (autonomous HC takes poison, uses on tea)
- Landscape analysis: `/tmp/llm-autonomy-landscape-2026.md`
