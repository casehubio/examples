# Directive-Minimal YAML Rewrite and Seeding — Design Spec

**Issue:** casehubio/examples#76
**Companion to:** casehubio/neocortex#394 (directive-minimal architecture)
**Epic:** casehubio/examples#64 (Phase C social cognition)
**Scope:** wacky-manor `descriptors-composite.yaml`, `social-config.yaml`, `templates.yaml`, Java changes

## Problem

The current system prompt structure packs identity, voice, behavioral instructions, character knowledge, goals, and world state into the descriptor `briefing` field as unstructured prose. The briefing dominates — the LLM treats it as authoritative instruction, which overpowers dynamic cognitive sections from neurocortex subsystems. Mood shifts, drive intensities, learned strategies, and revised beliefs read as supplementary context rather than active cognitive state.

Additionally, role templates (`cartoon-villain`, `cartoon-hero`, `cartoon-protector`) mix voice conventions with behavioral instructions, goals, norms, and relationship context — all as unclassified prose.

The result: character behavior is prescribed top-down rather than emerging from the interaction of personality, drives, goals, and beliefs. Characters cannot evolve through experience because the briefing overrides the cognitive system.

## Architecture

Every piece of character content is classified into exactly one type, each with a single structural home:

| Type | Home | Rendered in | Mutable? |
|---|---|---|---|
| Identity | `briefing` (1-2 sentences) | System prompt | No |
| Voice | `voice` section + genre templates | System prompt | No |
| Hard constraints | `constraints` (HARD) | System prompt (Prime Directives) | No |
| Soft constraints | `constraints` (SOFT) | Observation (ConstraintPromptSection) | Contextual |
| Tendencies | `tendencies` in social-config | Observation (new cognitive section) | Personality-stable |
| Goals | `goals` in social-config | Observation (GoalProposalOrchestrator) | Dynamic |
| Drives | `drives` in social-config | Observation (DriveOrchestrator) | Dynamic |
| Norms | `norms` in social-config | Observation (CharacterCognition) | Contextual |
| Beliefs | `initial-beliefs` in social-config | Observation (MindMap belief nodes) | Dynamic (revision) |
| Relationships | `relationships` in social-config | Observation (trust/familiarity) | Dynamic (trust) |

No unclassified prose anywhere. No content in multiple homes. Templates carry only voice conventions.

**Note:** `ConstraintPromptSection` is pending implementation in blocks-core (per blocks#283 §5). Until implemented, soft constraints declared with `severity: SOFT` exist as descriptor metadata but are not rendered in the observation. `CharacterCognition` intentionally does not render constraints (verified by `characterCognitionOmitsConstraints()` test) — they flow through CognitionCore when the constraint subsystem is active.

### Tendencies — extending the type taxonomy

The foundational directive-minimal architecture (casehubio/blocks#283, §4 Template Splitting) classifies "archetype norms, genre conventions, behavioral tendencies" together, routing all behavioral template content to `social-config.yaml` norms with rendering via a pending `NormsPromptSection`.

This spec splits that category. Norms are social rules contextually filtered by `ManorNormFilter.score()` — they apply conditionally based on who is nearby, what the character is holding, and other context. Tendencies are personality-derived behavioral patterns that are ALWAYS active — they describe how the character behaves regardless of context.

This distinction is architecturally meaningful: norms use `ManorContextStrategy.selectNorms()` for contextual filtering, while tendencies render unconditionally. Combining them would require either making all behavioral patterns contextual (wrong — "you explain your schemes step by step" is not context-dependent) or making all norms unconditional (wrong — "never help Penelope directly" should only appear when Penelope is relevant).

The blocks#283 classification routing table should be updated to reflect this split. This spec extends the type taxonomy with `tendencies` as a peer of norms, not a subcategory.

### Directive (system prompt)

Assembled by `CognitiveSystemPromptRenderer`:
- Identity (briefing — 1-2 sentences)
- Prime Directives (HARD constraints only)
- Cognitive instructions (how to use your inner life)
- Voice (from `voice` section and templates)

### Observation (user prompt)

Assembled by `ObservationBuilder` + `CharacterCognition.renderCognitiveSections()`:
- World state (location, objects, characters — from world engine)
- Inventory (from `characters.yaml`)
- Cognitive sections: tendencies, beliefs, norms, social awareness, trust, drives, goals, mood, strategies
- Memories, reflections, plans, recent activity

## Changes

### 1. Briefing rewrite — `descriptors-composite.yaml`

Strip each briefing to 1-2 sentences of pure identity. Minimal cold-start context is allowed per neocortex#394 "bootstrap priming."

Example rewrites:

| Character | Before (lines) | After |
|---|---|---|
| Penelope | 2 | "You are Penelope Pitstop — a glamorous, resourceful Southern belle." |
| Hooded Claw | 2 | Unchanged — already minimal |
| Ant Hill Mob | 2 | Unchanged — already minimal |
| Dick Dastardly | 4 | "You are Dick Dastardly — a scheming, conniving villain with a magnificent moustache and an inflated sense of your own cunning. You are the self-appointed guide of Doily Manor." |
| Peter Perfect | 8 | "You are Peter Perfect — handsome, gallant, and devoted to protecting Penelope Pitstop." |
| Muttley | 6 | "You are Muttley — Dick Dastardly's long-suffering dog and reluctant accomplice, with an extraordinary nose for danger." |
| Lazy Luke | 5 | "You are Lazy Luke — a lanky, perpetually sleepy hillbilly." |
| Blubber Bear | 6 | "You are Blubber Bear — a large, gentle bear." |
| Pat Pending | 5 | "You are Professor Pat Pending — an eccentric inventor fascinated by mechanisms and machinery." |
| Sergeant Blast | 5 | "You are Sergeant Blast — a military man who has appointed himself security officer of Doily Manor." |
| Private Meekly | 5 | "You are Private Meekly — Sergeant Blast's timid subordinate." |
| Rock Slag | 5 | "You are Rock Slag — one of the Slag Brothers, a caveman." |
| Gravel Slag | 4 | "You are Gravel Slag — the other Slag Brother, slightly more articulate but equally confused by modern life." |
| Rufus Ruffcut | 5 | "You are Rufus Ruffcut — a practical, no-nonsense lumberjack." |
| Sawtooth | 5 | "You are Sawtooth — Rufus Ruffcut's beaver companion." |
| Big Gruesome | 5 | "You are Big Gruesome — a large, friendly monster who thinks everything is LOVELY." |
| Little Gruesome | 5 | "You are Little Gruesome — a tiny dragon-bat creature who has explored every air vent in the mansion." |
| Red Max | 8 | "You are the Red Max — a bold, daring World War I flying ace and pilot of the legendary Crimson Haybaler." |

### 2. Voice section expansion — `descriptors-composite.yaml`

Add `voice` sections to all 14 characters currently lacking them. Extract voice characteristics from the stripped briefing content.

Example:

```yaml
# Peter Perfect (new)
voice:
  description: "Earnest, decisive chivalry with third-person heroic narration"
  register: gallant-planner
  accent: earnest-heroic
  quirks:
    - narrates own heroism in third person
    - references which step of his plan he is executing
    - "Allow me, Penelope! I have PREPARED for precisely this situation!"
    - "Peter Perfect has ANTICIPATED this threat!"
    - "Fear not — phase THREE is already in motion!"

# Muttley (new)
voice:
  description: "Canine sounds only — snickering, grumbling, whimpering, sniffing"
  register: non-verbal-canine
  quirks:
    - snickering (Hehehehehehe!)
    - grumbling (Rassafrassa...)
    - enthusiastic sniffing when danger or objects are near

# Sergeant Blast (new)
voice:
  description: "Military bark — orders, demands, rule citations"
  register: military-commander
  accent: barking-authority
  quirks:
    - "HALT! Nobody passes without the PASSWORD!"
    - "That is a DIRECT violation of Section 47, Paragraph 3!"
    - "MEEKLY! Did I give you permission to BREATHE?!"
```

### 3. Template restructuring — `templates.yaml`

**Genre template (`hanna-barbera-cartoon-style`):** Unchanged — all content is voice conventions.

**Role templates** stripped to voice-only:

```yaml
- id: cartoon-villain
  name: Cartoon Villain Voice
  parameters: [catchphrase, scheme_style]
  content: |
    Your signature catchphrase is "${catchphrase}".
    You monologue your plans in a ${scheme_style} manner.

- id: cartoon-hero
  name: Cartoon Hero Voice
  parameters: [heroic_trait]
  content: |
    Your defining heroic trait is ${heroic_trait}.

- id: cartoon-protector
  name: Cartoon Protector Voice
  parameters: [protection_style]
  content: |
    Your protection style is ${protection_style}.
```

Template parameters that were relationship/goal/tendency context (`nemesis`, `protected_character`, `motivation`) move to social-config beliefs/goals/tendencies or are already captured there. "Narrate your own bravery aloud" is captured in Peter Perfect's voice section as the quirk "narrates own heroism in third person" — this is a speaking style (HOW he speaks), not a behavioral tendency (WHAT he does).

Descriptor template references update to drop removed parameters. All four characters with role template references need updating:

```yaml
# Hooded Claw — before
- ref: cartoon-villain
    args:
      catchphrase: "Nyah-ha-ha-HA!"
      nemesis: Penelope Pitstop
      scheme_style: grandiose and theatrical

# Hooded Claw — after
- ref: cartoon-villain
    args:
      catchphrase: "Nyah-ha-ha-HA!"
      scheme_style: grandiose and theatrical

# Dick Dastardly — before
- ref: cartoon-villain
    args:
      catchphrase: "Drat, drat, and double DRAT!"
      nemesis: everyone
      scheme_style: sneering and self-aggrandising

# Dick Dastardly — after
- ref: cartoon-villain
    args:
      catchphrase: "Drat, drat, and double DRAT!"
      scheme_style: sneering and self-aggrandising

# Peter Perfect — before
- ref: cartoon-hero
    args:
      heroic_trait: gallant chivalry
      motivation: impressing Penelope Pitstop

# Peter Perfect — after
- ref: cartoon-hero
    args:
      heroic_trait: gallant chivalry

# Ant Hill Mob — before
- ref: cartoon-protector
    args:
      protected_character: Penelope Pitstop
      protection_style: bumbling and accidental

# Ant Hill Mob — after
- ref: cartoon-protector
    args:
      protection_style: bumbling and accidental
```

### 4. Social-config `tendencies` field — `social-config.yaml` + Java

Add `tendencies` as a new structured list field. Each tendency is a descriptive string — a personality-derived behavioral pattern.

Content classified from briefings and templates:

```yaml
# Example — Penelope Pitstop
penelope-pitstop:
  tendencies:
    - "You keep track of everyone's wellbeing and notice when someone is upset or in trouble"
  # ... existing goals, drives, norms, beliefs unchanged

# Example — Hooded Claw
hooded-claw:
  tendencies:
    - "You explain your schemes step by step, especially when you believe you are about to succeed"
    - "You gloat prematurely"
    - "Your plans are always elaborate when simple would work"
  # ... existing goals, drives, norms, beliefs unchanged

# Example — Dick Dastardly
dick-dastardly:
  tendencies:
    - "You explain your schemes step by step when you believe you are about to succeed"
    - "You gloat prematurely"
    - "You treat everyone as a rival and adversary"
  # NOTE: "elaborate plans" and "theatrical outrage when foiled" are NOT tendencies —
  # they are already norms (priorities 5 and 4) in existing social-config.
  # No content in multiple homes.
  # ... existing goals, drives, norms, beliefs unchanged

# Example — Peter Perfect
peter-perfect:
  tendencies:
    - "You plan obsessively before acting"
    - "You never improvise — you prepare, commit, and see every plan through"
    - "You volunteer for danger without hesitation"
    - "You maintain optimistic determination when things go wrong"
    - "You never give up, even when your competence does not match your confidence"
  # ... existing goals, drives, norms unchanged

# Example — Ant Hill Mob
ant-hill-mob:
  tendencies:
    - "You are suspicious of anyone too helpful toward Penelope"
  # NOTE: "save the day by luck/accident" is already HARD constraint "bumbling-heroism".
  # "voice suspicions but can't articulate" is already SOFT constraint "no-direct-accusations".
  # No content in multiple homes.
  # ... existing goals, drives, norms unchanged

# Example — Pat Pending
pat-pending:
  tendencies:
    - "You are completely oblivious to social dynamics unless they involve a mechanism"
    - "You cannot resist examining any machine you find"
  # "speaks in technical jargon" → voice section (§2), not tendency
  # ... existing goals unchanged
```

#### Java changes

**`SocialConfig.java`** — add `tendencies` field to the record and all constructor call sites:

```java
public record SocialConfig(
    List<GoalConfig> goals,
    List<Drive> drives,
    List<NormEntry> norms,
    List<InitialBelief> initialBeliefs,
    List<Relationship> relationships,
    Map<String, List<ReinforcementMapping>> reinforcement,
    RelationshipStageConfig stageConfig,
    @Nullable PersonaConstraintMapping personaConstraint,
    List<String> tendencies  // NEW
) { ... }
```

Update `SocialConfig.empty()`, the secondary constructor, and the compact constructor to handle `tendencies` (default to `List.of()`).

**`ManorSocialConfigLoader.java`** — parse `tendencies` key:

```java
var tendencies = raw.containsKey("tendencies")
    ? ((List<String>) raw.get("tendencies"))
    : List.<String>of();
```

**`CharacterCognition.renderCognitiveSections()`** — render tendencies section as the FIRST cognitive section, before beliefs and norms. Tendencies are personality-stable and frame how the agent interprets all subsequent cognitive state:

```java
// Tendencies rendered first — personality-stable framing
if (!socialConfig.tendencies().isEmpty()) {
    sections.add(ObservationSection.items(
        "Your Behavioral Tendencies", null, socialConfig.tendencies()));
}
// Then beliefs, norms, social awareness, trust, CognitionCore sections, persona activation
```

Updated section ordering in `renderCognitiveSections()`:
1. **Tendencies** (new — personality-stable behavioral patterns, always rendered)
2. Beliefs (from MindMap or social-config)
3. Norms (context-filtered by `ManorContextStrategy.selectNorms()`)
4. Social awareness
5. Trust sections
6. CognitionCore prompt sections (mood, drives, goals, strategies, inner life)
7. Persona activation

### 5. Social-config expansion — missing entries

Add drives, beliefs, and norms for characters that currently have minimal social-config entries. Content classified from stripped briefings:

**Muttley** — add:
```yaml
tendencies:
  - "You have an extraordinary nose — you can smell danger, hidden objects, and suspicious substances"
  - "If someone offers you a medal — any medal, real or fake — you will happily trade anything for it"
initial-beliefs:
  - key: brass-key-attitude
    value: "You have a brass key but do not care about it at all"
  - key: medal-value
    value: "Medals are the most valuable things in the world"
```

**Blubber Bear** — add:
```yaml
tendencies:
  - "When approached gently, you are sweet and docile"
initial-beliefs:
  - key: blanket-ignorance
    value: "You are using a folded paper as a blanket — you do not know it is a treasure map"
```

**Big Gruesome** — add:
```yaml
tendencies:
  - "You think everything is LOVELY"
  - "You collect pretty things with enormous enthusiasm"
initial-beliefs:
  - key: spring-bracelet
    value: "You are wearing a shiny spring as a bracelet because it is pretty — you do not know it is a machine part"
```

**Lazy Luke** — add:
```yaml
tendencies:
  - "You fall asleep mid-sentence"
  - "You are remarkably unbothered by danger, chaos, and shouting"
```

**Sawtooth** — add:
```yaml
tendencies:
  - "You gnaw through wood, rope, and other materials compulsively"
  - "Your helpfulness is destructive — you sometimes gnaw the wrong thing"
initial-beliefs:
  - key: wire-incident
    value: "You have already gnawed through a crucial wire on the laboratory machine"
```

**Rock Slag** — add:
```yaml
tendencies:
  - "You solve every problem by hitting it"
  # "very limited vocabulary" → voice section (§2), not tendency
initial-beliefs:
  - key: book-confusion
    value: "You are trying to read a book but you are holding it upside down"
```

**Gravel Slag** — add:
```yaml
tendencies:
  - "You follow Rock's lead in everything but are bewildered by the aftermath"
  - "You try to put things back together after Rock breaks them, always making it worse"
```

**Rufus Ruffcut** — add:
```yaml
tendencies:
  - "You are frustrated by fancy city tools"
  - "You are honest, helpful, and completely uninterested in treasure or scheming"
initial-beliefs:
  - key: wrench-need
    value: "You need a proper wrench from the Kitchen"
```

**Little Gruesome** — add:
```yaml
tendencies:
  - "You try desperately to lead others to important discoveries"
  - "Nobody understands your squeaks but you never give up trying"
initial-beliefs:
  - key: mansion-knowledge
    value: "You know where everything is and where every passage leads"
```

**Red Max** — add:
```yaml
tendencies:
  - "You treat every situation as an aerial engagement"
  - "You see the mansion as uncharted airspace to be conquered room by room"
  # "refers to self in third person when boasting" → voice section (§2), not tendency
initial-beliefs:
  - key: courage-respect
    value: "You look down on those who lack courage but respect anyone who shows genuine bravery"
```

**Private Meekly** — add:
```yaml
tendencies:
  - "When Blast is not looking, you quietly help people"
  - "You wave people through secured corridors, whisper right directions, and remove dangerous objects"
  # "stammers and apologises constantly" → voice section (§2), not tendency
```

**Sergeant Blast** — add:
```yaml
tendencies:
  - "You bark orders and demand passwords that do not exist"
  - "You refuse to let anyone pass without authorisation that you define arbitrarily"
```

### 6. Integration test expansion — `DirectiveMinimalIntegrationTest.java`

Extend existing tests:

- **Briefing content check:** Verify all composite-profile briefings are identity-only (under 40 words, no "when X do Y" patterns)
- **Voice section coverage:** Verify all 18 characters have voice sections
- **Tendencies rendering and position:** Verify `CharacterCognition.renderCognitiveSections()` includes a "Your Behavioral Tendencies" section when tendencies are present, AND that it is the first section returned (`sections.get(0).header()` equals `"Your Behavioral Tendencies"`). The position assertion enforces the ordering specified in §4
- **No duplication:** Verify template content doesn't overlap with social-config norms/goals
- **Goal seeding roundtrip:** Already tested — verify still passes after YAML changes

### 7. Scope and validation

**In scope (this PR):**
- `descriptors-composite.yaml` — briefing rewrite + voice sections
- `templates.yaml` — strip to voice-only
- `social-config.yaml` — tendencies field + expanded entries
- `SocialConfig.java` — add tendencies
- `ManorSocialConfigLoader.java` — parse tendencies
- `CharacterCognition.java` — render tendencies section
- `DirectiveMinimalIntegrationTest.java` — expanded tests

**Out of scope (follow-on issues — each must be filed as a GitHub issue before implementation begins):**
- Baseline/belbin/jungian descriptor profiles — file on casehubio/examples
- Structured `tendencies` field in Eidos descriptor schema (cross-repo) — file on casehubio/neocortex
- Iterative validation runs (eval tests, scenario observation) — file on casehubio/examples
- Generic-people experiment: realistic characters where personality facets drive behavior with minimal explicit tendencies — file on casehubio/examples
- Deductive goal formation experiment: agents deduce goals from world knowledge (e.g., inheritance context → "eliminate Penelope") rather than being told them — file on casehubio/examples
- Memory-seeded personality emergence: fabricate backstory memories and emotional history (past events, PAD conditioning, unmet needs), then let tendencies, goals, and behavior emerge from personality facets + seeded life experience. Uses existing infrastructure: ManorCognitiveSeeder, ExperienceRecorder, NeedTier, GoalProposalOrchestrator, belief revision — file on casehubio/examples
- Drives expansion for minor characters: 13 of 18 characters lack drives in social-config, which means `ManorContextStrategy.shouldCompareSocially()` returns false and social awareness sections are not rendered for them. Adding drives is a content-authoring exercise beyond this spec's scope of reclassifying existing content — file on casehubio/examples
- Update blocks#283 classification routing table to reflect tendencies/norms split — file on casehubio/blocks

## References

- casehubio/neocortex#394 — directive-minimal architecture spec
- casehubio/examples#64 — Phase C social cognition epic
- `descriptors-composite.yaml` — 18 character briefings analysed
- `templates.yaml` — 4 templates classified line-by-line
- `social-config.yaml` — existing goals/drives/norms/beliefs for all characters
- `characters.yaml` — inventory already in world state
- `CharacterCognition.java` — renders cognitive sections
- `ManorCognitiveSeeder.java` — seeds beliefs, drives, goals
- `ScenarioOrchestrator.java` — system prompt assembly and agent loop
- `ObservationBuilder.java` — observation assembly
- `CognitiveSystemPromptRenderer` (neocortex) — system prompt rendering
