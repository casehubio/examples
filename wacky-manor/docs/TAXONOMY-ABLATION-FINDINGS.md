# Taxonomy Ablation Findings

Systematic experiment to find the minimum character taxonomy that maintains distinctive LLM agent behavior. Conducted on the Wacky Manor multi-agent scenario (issue #97).

## Objective

Determine which elements of the character taxonomy (drives, tendencies, speech-patterns, constraints, disposition) are load-bearing vs redundant. Strip elements iteratively, measure behavioral drift via 300+ event autonomous scenario runs.

## Method

Each round removes one category of taxonomy element from the GENERIC profile (renamed characters, no pop-culture priors). Run the autonomous scenario, capture full transcript, measure behavioral markers against baseline. The BASELINE profile (Wacky Races characters) runs in parallel where relevant to detect model-prior compensation.

Behavioral markers measured per character:
- **Scheme narration rate** (villains): percentage of aside events containing step-by-step scheme narration keywords
- **Planning rate** (Hartwell): percentage of events with planning keywords
- **Optimism rate** (Hartwell): percentage of events with optimistic determination keywords
- **Third-person narration rate** (Hartwell): percentage of events using self-referential third-person

## Rounds and Results

### Round 1 — Strip 7 redundant directives

**Removed:** 7 tendencies that restated what drives or disposition already specified (e.g., "scheme elaborately" when drives already include scheming at 0.9).

**Result:** Zero behavioral drift across all characters. Drives + disposition carry personality traits. Tendencies that restate drives are pure redundancy.

**Conclusion:** Safe to remove. Drives are the authoritative source for personality traits.

### Round 2 — Strip ALL villain tendencies

**Removed:** All tendencies from Marsh (hooded-claw) and Foxworth (dick-dastardly).

**Result:** Asymmetric degradation.

| Character | Profile | Scheme narration baseline | After strip | Delta |
|---|---|---|---|---|
| Marsh | GENERIC | ~50% | ~14% | -36pp |
| Hooded Claw | BASELINE | ~50% | ~31% | -19pp |

The BASELINE profile (Hooded Claw) degraded less because the model has Wacky Races priors — it "knows" the Hooded Claw narrates schemes. The GENERIC profile (Marsh) has no such priors, so the drop is larger.

**Key finding:** "Explain schemes step by step" is a *storytelling convention*, not a personality trait. Real people don't narrate plans aloud — cartoon characters do for the audience. Storytelling conventions cannot emerge from drives alone.

### Round 2b — Encode narration convention in drive description

**Changed:** Added "and narrate them step by step aloud" to the scheming drive description.

**Result:** Recovered Marsh scheme narration. But only when drive intensity was 0.85+. Foxworth at 0.7 intensity did not recover; bumped to 0.85 and recovery confirmed (29.6% in emotional-core run).

**Key finding:** Drive descriptions can carry storytelling conventions, but only above an intensity threshold (~0.85). Below that, the model treats the description as background context rather than a behavioral imperative.

### Emotional-core experiment

**Changed:** Rewrote Marsh's drive descriptions from behavioral directives to internal emotional states:
- "Compelled to hatch elaborate plans" → "You feel contemptuous glee when outsmarting someone"
- "Must protect cover at all costs" → "You feel paranoid vigilance when your cover might be exposed"
- "Enjoys dominating others" → "You feel cold satisfaction when you hold power someone does not know you have"
- "Cannot resist gloating" → "You feel an intoxicating rush of premature triumph that overwhelms your better judgment"

**Result (375 events, clean full run):**

| Metric | Behavioral drives (R2b) | Emotional-core | Delta |
|---|---|---|---|
| Scheme narration rate | 55.0% | 11.5% | -43.5pp |
| Unique emotion words | 5 | 9 | +80% |

Emotional-core framing produces richer vocabulary (2x unique emotion words) and more varied self-characterisation. Marsh's asides shifted from formulaic "Step one... Step two..." planning to visceral internal monologue. The improvement is real but incremental — both framings produce good theatrical villainy. Not the dramatic transformation the truncated initial run (214 events) had suggested.

### Round 3 — Strip Hartwell tendencies

**Removed:** Two Peter/Hartwell tendencies:
- "You plan obsessively before acting"
- "You maintain optimistic determination when things go wrong"

**Result (325 events):** Asymmetric degradation across markers.

| Marker | Baseline | R3 (stripped) | Delta | Verdict |
|---|---|---|---|---|
| Planning | 16.7% | 26.1% | +9.4pp | Survived (drives carry it) |
| Optimism | 24.1% | 2.2% | -21.9pp | Collapsed |
| Third-person narration | 61.1% | 13.0% | -48.1pp | Collapsed |

**Key findings:**
1. **Tendencies function as character anchors**, not just behavioral instructions. They reinforce the broader voice pattern including elements specified elsewhere (speech-pattern).
2. **Speech-patterns alone don't override model defaults.** The speech-pattern still said "narrates his own actions in the third person" — but without the tendency reinforcing it, the model reverted to its default first-person voice.
3. **Optimism isn't carried by drives.** The `proving-worth` drive covers competence-seeking but not the emotional resilience of optimistic determination. Hartwell shifted to anxiety and self-doubt.

### Round 3b — Character-reflection instruction

**Changed:** Modified the response format thinking field to instruct: "FIRST review your voice (accent, register, speech pattern), drives, and constraints — then reason about the current situation."

**Hypothesis:** If the model actively consults its character attributes before each response, the speech-pattern and drives become sufficient without tendency reinforcement. The problem in Round 3 wasn't that attributes were missing — they were present but passively ignored. Metacognitive prompting forces active recall.

**Result (312 events):**

| Marker | Baseline | R3 (no reflection) | R3b (with reflection) | Recovery? |
|---|---|---|---|---|
| Planning | 16.7% | 26.1% | 27.3% | N/A — never collapsed |
| Optimism | 24.1% | 2.2% | 13.6% | Partial (+11.4pp, ~half recovered) |
| Third-person | 61.1% | 13.0% | 61.4% | **Full** (back to baseline) |

**Key findings:**
1. **Third-person narration: full recovery.** 13.0% → 61.4% — back to exactly the baseline. One line in the thinking step completely solved the speech-pattern override problem. The model just needed to be told to actively consult its voice spec.
2. **Optimism: partial recovery.** 2.2% → 13.6% — significant improvement but still 10pp below baseline. The reflection instruction brings back voice/style attributes, but optimistic determination is a *behavioral response pattern* (how you react to setbacks), not a voice attribute the model can "review."
3. **The metacognitive prompt splits the taxonomy cleanly:** voice-mode attributes (how you sound) are fully recoverable via active recall. Behavioral dispositions (how you react) still need tendencies or drive encoding.

## Taxonomy Category Model

| Category | Can emerge from drives? | Where it belongs | Evidence |
|---|---|---|---|
| Personality trait (scheming, gloating) | Yes | Drives | Round 1: all 7 removals safe |
| Storytelling convention (narrate plans aloud) | No — medium-specific | Drive description at 0.85+ intensity | Round 2/2b |
| Voice texture (accent, catchphrases) | Partially | Voice section (style-directive) | Style-directive experiment |
| Internal emotional state | Produces varied external behavior | Drives (emotional-core framing) | Emotional-core: 2x vocabulary |
| Prescribed anchor (evil laugh) | EMERGENCE-GAP — needs neocortex | Voice signature-phrases | Hooded Claw vs Marsh comparison |
| Voice mode (third-person, register shifts) | Yes, with metacognitive prompt | Speech-pattern + character-reflection | Round 3b: full recovery |
| Behavioral disposition (optimism, resilience) | Partially | Tendency (partially load-bearing) OR emotional drive | Round 3b: partial recovery |

## Design Principles

| # | Principle | Evidence |
|---|---|---|
| 1 | **Drives carry personality traits** | Round 1: all 7 removals safe |
| 2 | **Drives don't carry storytelling conventions** | Round 2: scheme narration dropped without explicit encoding |
| 3 | **Drive intensity gates convention-carrying** | R2b: 0.85 works, 0.7 doesn't |
| 4 | **Direct at a region, not a point** | Emotional-core: emotional framing broadens vocabulary without prescribing specific words |
| 5 | **Storytelling conventions ≠ personality traits** | Round 2: asymmetric degradation between Wacky Races (model priors) and generic (no priors) |
| 6 | **Tendencies anchor voice mode** | Round 3: third-person narration collapsed without tendency despite speech-pattern |
| 7 | **Speech-patterns don't override model defaults alone** | Round 3: speech-pattern alone insufficient for third-person |
| 8 | **LLMs prioritise goals over personality** | Round 3: goal-directed behavior persisted, ambient personality attributes drifted |
| 9 | **Emotional-state framing broadens vocabulary** | Emotional-core: 2x unique emotion words vs behavioral directives |
| 10 | **Metacognitive prompting recovers voice attributes** | Round 3b: third-person narration fully recovered (13% → 61.4%) |
| 11 | **Behavioral dispositions need explicit encoding** | Round 3b: optimism only partially recovered (2.2% → 13.6% vs 24.1% baseline) |

## Open Questions

1. **Can behavioral dispositions be encoded as emotional drives?** Optimism partially recovered via metacognitive prompting but not fully. Would "You feel stubborn optimism that rises when things go wrong" as a drive at 0.85+ intensity close the remaining 10pp gap?

2. **Should emotional-core framing apply to all characters?** The improvement is incremental but consistent. The risk is making many changes at once, complicating regression analysis.

3. **What about the BASELINE (Wacky Races) profile?** All ablation done on GENERIC only. The BASELINE profile benefits from model priors (the model "knows" Penelope Pitstop, Dick Dastardly, etc.), which mask taxonomy weaknesses. Changes validated on GENERIC should transfer, but model priors add an uncontrolled variable.

4. **Can the intensity threshold be characterised more precisely?** We know 0.85 works and 0.7 doesn't for convention-carrying. Is the threshold consistent across different types of conventions, or does it vary?

5. **What happens to model-prior compensation as models are updated?** The BASELINE profile relies partially on model knowledge of Wacky Races characters. If a future model has weaker priors for these characters, the BASELINE profile may need the same reinforcement the GENERIC profile needs.

## Transcripts

All transcripts in `wacky-manor/docs/eval/`:

| Transcript | Events | Round | Profile |
|---|---|---|---|
| `new-briefings-20261002/` | 333 | Original baseline | BASELINE |
| `generic-colloquial-20261002/` | 310 | Original baseline | GENERIC |
| `pareback-r1-baseline-20261003/` | 331 | Round 1 | BASELINE |
| `pareback-r1-generic-20261003/` | 315 | Round 1 | GENERIC |
| `pareback-r2-baseline-20261003/` | 311 | Round 2 | BASELINE |
| `pareback-r2-generic-20261003/` | 334 | Round 2 | GENERIC |
| `pareback-r2b-baseline-20261003/` | 315 | Round 2b | BASELINE |
| `pareback-r2b-generic-20261003/` | 322 | Round 2b | GENERIC |
| `emotional-core-generic-20261003/` | 214 | Emotional-core (truncated) | GENERIC |
| `emotional-core-generic-full-20261003/` | 375 | Emotional-core (full) | GENERIC |
| `pareback-r3-generic-20261003/` | 325 | Round 3 | GENERIC |
| `pareback-r3b-generic-20261003/` | 312 | Round 3b (character-reflection) | GENERIC |
| `old-briefings-20261002/` | 327 | Pre-rewrite reference | BASELINE |
| `generic-briefings-20261002/` | 306 | Prescribed catchphrases reference | GENERIC |
