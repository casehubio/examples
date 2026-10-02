## D1: Briefing scope — identity only

**Choice:** Strip briefings to 1-2 sentences of pure identity. Behavioral tendencies move to a structured `tendencies` field in social-config (see D7), not prose in the briefing.

**Alternatives:**
- Identity + behavioral tendencies in briefing prose — tendencies are unclassified prose, defeats structural goal
- Status quo (full behavioral scripts) — prevents emergence, duplicates social-config

**Rationale:** Every piece of character content must be typed and in one structural home. Briefing carries only identity ("You are Peter Perfect — handsome, gallant, devoted to Penelope Pitstop"). Cold-start scenario context stays as identity flavor per neocortex#394 bootstrap priming.

**Trade-offs:** Briefing is very minimal — character distinctiveness comes from voice + tendencies + drives, not briefing prose.

**Sources:** neocortex#394 directive-minimal architecture, descriptors-composite.yaml
**Exploration:** quick
**Status:** captured

## D2: Voice section expansion

**Choice:** Add structured `voice` sections to all 13 characters currently lacking them. Voice captures register, accent, and quirks (catchphrases, speaking patterns).

**Alternatives:**
- Leave voice content in briefings — conflicts with directive-minimal
- Put catchphrases in template args — too rigid, not all characters use role templates

**Rationale:** 13 of 17 characters have no voice section. Their voice is currently ONLY in briefing text. Non-verbal characters (Muttley, Blubber Bear, Sawtooth, Little Gruesome) get voice sections describing their specific sounds.

**Trade-offs:** More YAML to maintain. Worth it for clean separation.

**Sources:** descriptors-composite.yaml — only Penelope, Hooded Claw, Ant Hill Mob, Dick Dastardly have voice sections
**Exploration:** quick
**Status:** captured

## D3: Social-config expansion for stripped content

**Choice:** Add missing drives, initial-beliefs, and targeted norms for characters whose briefing/template content is being stripped. Classify each piece into its correct type — no unclassified content.

**Alternatives:**
- Move all behavioral content to norms — wrong type for tendencies and beliefs
- Delete everything and rely on cognitive system — too aggressive for cartoon characters

**Rationale:** Every piece of content gets a type: norms for social rules, beliefs for starting knowledge, drives for motivational forces, tendencies for behavioral patterns.

**Trade-offs:** Rich cognitive sections may drown out personality profile facets. Wacky-manor characters are cartoon archetypes where explicit tendencies do the heavy lifting — personality-facet-driven behavior is a different experiment.

**Sources:** social-config.yaml, briefings, templates — cross-referenced
**Exploration:** quick
**Status:** captured

## D4: Templates become voice-only

**Choice:** Strip ALL non-voice content from role templates (cartoon-villain, cartoon-hero, cartoon-protector). Templates carry ONLY voice conventions. All behavioral content (tendencies, goals, norms, beliefs) moves to typed fields in social-config.

Genre template (hanna-barbera-cartoon-style) stays as-is — it's all voice conventions for the world.

**Alternatives:**
- Keep templates as mixed prose — unclassified content, defeats structural goal
- Add typed sections to templates (voice/tendencies/norms) — requires Eidos template schema change
- Remove role templates entirely — loses parameterized voice (catchphrase, scheme_style)

**Rationale:** Templates were the last prose bucket with unclassified content. Moving behavioral content to typed social-config fields means every piece of character data is classified. Templates keep their value for composable, parameterized voice conventions.

**Depends on:** D3 (social-config expansion), D7 (tendencies field)
**Sources:** templates.yaml line-by-line classification
**Exploration:** quick
**Status:** captured

## D5: Iterative validation approach

**Choice:** Implement the rewrite, run the scenario, observe where characters lose distinctiveness, adjust. Empirical validation over paper perfection.

**Alternatives:**
- Perfect-on-paper — impossible to predict LLM behavior from configuration
- Ship without validation — risky for character quality

**Rationale:** The iterative loop: rewrite → run → note → adjust → run. Eval tests provide structured feedback.

**Trade-offs:** Multiple iteration cycles. Worth it.

**Sources:** User direction, neocortex#394 emergence goals
**Exploration:** quick
**Status:** captured

## D6: Scope — composite profile only

**Choice:** Focus on descriptors-composite.yaml. Baseline/belbin/jungian updated in follow-on.

**Alternatives:**
- All 4 profiles — larger scope, more risk

**Rationale:** Composite is production. Experiment profiles follow the same pattern separately.

**Trade-offs:** Experiment profiles inconsistent until follow-on. Acceptable for pre-release.

**Sources:** ProfileAwareDescriptorRegistrar.java
**Exploration:** quick
**Status:** captured

## D7: Tendencies as structured social-config field

**Choice:** Add `tendencies` as a new structured list field in social-config, alongside goals/drives/norms/beliefs. Tendencies are personality-derived behavioral patterns ("gloats prematurely", "plans obsessively"). Rendered in the observation as a cognitive section.

**Alternatives:**
- Tendencies in briefing prose — unclassified, not structural
- Tendencies in templates — templates become mixed prose buckets again
- New `tendencies` field in Eidos descriptor (system prompt) — requires cross-repo schema change, and system-prompt tendencies override beliefs (bad for emergence)

**Rationale:** Observation-side tendencies are better for emergence: a tendency to "gloat prematurely" alongside a belief "gloating has gotten me caught" creates productive tension the LLM resolves. System-prompt tendencies would override the belief. Keeps everything typed and in one structural home. No Eidos schema changes needed — social-config is wacky-manor-local.

**Trade-offs:** Tendencies in observation may be weighted less than if in system prompt. For cartoon archetypes this is acceptable — the tendencies list is prominent enough. For more nuanced characters, a descriptor-level field may be needed later.

**Sources:** Template classification analysis, emergence-first design principle
**Exploration:** quick
**Status:** captured

## D8: Content type taxonomy

**Choice:** Every piece of character content is classified into exactly one of these types:

| Type | Home | Rendered in | Mutable? |
|---|---|---|---|
| Identity | `briefing` (1-2 sentences) | System prompt | No |
| Voice | `voice` section + templates | System prompt | No |
| Hard constraints | `constraints` (HARD) | System prompt | No |
| Soft constraints | `constraints` (SOFT) | Observation | Contextual |
| Tendencies | `tendencies` in social-config | Observation | Personality-stable |
| Goals | `goals` in social-config | Observation | Dynamic |
| Drives | `drives` in social-config | Observation | Dynamic |
| Norms | `norms` in social-config | Observation | Contextual |
| Beliefs | `initial-beliefs` in social-config | Observation | Dynamic (revision) |
| Relationships | `relationships` in social-config | Observation | Dynamic (trust) |

No unclassified prose. No content in multiple homes. Templates carry only voice conventions.

**Alternatives:**
- Keep current mixed approach — content untyped, duplicated
- Fewer types (merge tendencies into norms or beliefs) — blurs distinct concepts

**Rationale:** Structural clarity. Each type has distinct semantics (mutability, rendering location, cognitive role). No ambiguity about where content belongs.

**Sources:** First-principles analysis of all 17 character briefings and 4 templates
**Exploration:** quick
**Status:** captured
