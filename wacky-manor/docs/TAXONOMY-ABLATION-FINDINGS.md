# Taxonomy Ablation Findings

Systematic experiment to find the minimum character taxonomy that maintains distinctive LLM agent behavior. Conducted on the Wacky Manor multi-agent scenario (issue #97).

## Objective

Determine which elements of the character taxonomy (drives, tendencies, speech-patterns, constraints, disposition) are load-bearing vs redundant. Strip elements iteratively, measure behavioral drift via 300+ event autonomous scenario runs. Then find the minimal metacognitive instruction that replaces the load-bearing elements.

## Method

Each round removes one category of taxonomy element from the GENERIC profile (renamed characters, no pop-culture priors). Run the autonomous scenario, capture full transcript, measure behavioral markers against baseline. The BASELINE profile (Wacky Races characters) runs in parallel where relevant to detect model-prior compensation.

Behavioral markers measured per character:
- **Scheme narration rate** (villains): percentage of aside events containing step-by-step scheme narration keywords
- **Planning rate** (Hartwell): percentage of events with planning keywords
- **Optimism rate** (Hartwell): percentage of events with optimistic determination keywords
- **Third-person narration rate** (Hartwell): percentage of events using self-referential third-person

All transcripts are preserved in `wacky-manor/docs/eval/` and each configuration change is a separate git commit.

## Phase 1 — What's Redundant? (Rounds 1–2b)

### Round 1 — Strip 7 redundant directives

**Removed:** 7 tendencies that restated what drives or disposition already specified (e.g., "scheme elaborately" when drives already include scheming at 0.9).

**Result:** Zero behavioral drift across all characters.

**Conclusion:** Drives are the authoritative source for personality traits. Tendencies that restate them are pure redundancy.

### Round 2 — Strip ALL villain tendencies

**Removed:** All tendencies from Marsh (hooded-claw) and Foxworth (dick-dastardly).

**Result:** Asymmetric degradation.

| Character | Profile | Scheme narration baseline | After strip | Delta |
|---|---|---|---|---|
| Marsh | GENERIC | ~50% | ~14% | -36pp |
| Hooded Claw | BASELINE | ~50% | ~31% | -19pp |

The BASELINE profile degraded less — the model has Wacky Races priors that partially compensate.

**Key finding:** "Explain schemes step by step" is a *storytelling convention*, not a personality trait. Conventions don't emerge from drives alone.

### Round 2b — Encode narration convention in drive description

**Changed:** Added "and narrate them step by step aloud" to the scheming drive description.

**Result:** Recovered Marsh scheme narration, but only at drive intensity 0.85+. Foxworth at 0.7 did not recover; bumped to 0.85 and confirmed (29.6% in subsequent runs).

**Key finding:** Drive descriptions can carry storytelling conventions, but only above an intensity threshold (~0.85).

### Emotional-core experiment

**Changed:** Rewrote Marsh's drive descriptions from behavioral directives to internal emotional states:
- "Compelled to hatch elaborate plans" → "You feel contemptuous glee when outsmarting someone"
- "Must protect cover at all costs" → "You feel paranoid vigilance when your cover might be exposed"

**Result (375 events):** Richer emotional vocabulary (2x unique emotion words), more varied self-characterisation. But not a dramatic transformation — both framings produce good theatrical villainy.

## Phase 2 — What's Load-Bearing? (Round 3)

### Round 3 — Strip Hartwell tendencies

**Removed:** Two Peter/Hartwell tendencies:
- "You plan obsessively before acting"
- "You maintain optimistic determination when things go wrong"

The speech-pattern still said "narrates his own actions in the third person."

**Result (325 events):** Asymmetric degradation across markers.

| Marker | Baseline | R3 (stripped) | Delta | Verdict |
|---|---|---|---|---|
| Planning | 16.7% | 26.1% | +9.4pp | Survived (drives carry it) |
| Optimism | 24.1% | 4.3% | -19.8pp | Collapsed |
| Third-person | 61.1% | 13.0% | -48.1pp | Collapsed |

**Key findings:**
1. **Tendencies function as character anchors**, not just behavioral instructions.
2. **Speech-patterns alone don't override model defaults.** The speech-pattern still said "narrates in third person" — the model saw it but didn't follow it.
3. **LLMs prioritise goals over personality.** Goal-directed behavior persisted while ambient personality attributes drifted.

## Phase 3 — Can Metacognitive Instructions Replace Tendencies? (Rounds 3b–3g)

This is where the experiment shifted from *what to strip* to *how to compensate*. Each round tests a different instruction in the thinking field, with Hartwell's tendencies still stripped. The question: can the right instruction recover the collapsed markers without restoring the tendencies?

### R3b — Passive review

**Instruction:** "FIRST review your voice (accent, register, speech pattern), drives, and constraints — then reason about the current situation."

**Hypothesis:** The model isn't missing information — the character spec is right there in the system prompt. The problem is inattention. A nudge to actively read it before reasoning should recover the collapsed markers.

**Result (312 events):**

| Marker | Baseline | R3 (none) | R3b (passive) |
|---|---|---|---|
| Planning | 16.7% | 26.1% | 27.3% |
| Optimism | 24.1% | 4.3% | 13.6% |
| Third-person | 61.1% | 13.0% | **61.4%** |

**Third-person: full recovery.** 13→61.4%, back to baseline. One line in the thinking step completely solved the speech-pattern override problem.

**Optimism: partial recovery.** 4.3→13.6%, significant improvement but 10pp below baseline.

**Analysis:** The passive review works for *presentational* attributes (how you sound — accent, speech pattern, third person) because the model can review and mechanically apply them. It doesn't fully work for *dispositional* attributes (how you react — optimism) because those require the model to be in a certain emotional state, not just to remember a rule.

### R3c — Show-workings (structured fields)

**Instruction:** "SHOW YOUR WORKINGS — before reasoning, state: MY ACCENT: [your accent]. MY SPEECH PATTERN: [your pattern]. MY STRONGEST DRIVE: [name and what it feels like]. Then reason."

**Hypothesis:** If the model has to *produce* the character attributes (like a student showing workings), the recall would be stronger than passive review. Filling in structured fields forces actual engagement rather than a nod.

**Result (306 events):**

| Marker | Baseline | R3b (passive) | R3c (workings) |
|---|---|---|---|
| Planning | 16.7% | 27.3% | 15.9% |
| Optimism | 24.1% | 13.6% | 13.6% |
| Third-person | 61.1% | 61.4% | 50.0% |

**Show-workings was WORSE than passive review.** Third-person regressed from 61.4% to 50.0%.

**Analysis:** The model treated filling in "MY SPEECH PATTERN: third person" as *completing the review task* rather than *preparing to apply it*. Structured fields create a satisficing shortcut — the model can pattern-complete the metadata without it influencing downstream generation. The homework analogy breaks for LLMs: a student who shows workings is forced to *think through* the steps. An LLM filling structured fields is just pattern-completing.

**Principle established:** Open-ended prompts outperform structured ones for attribute integration.

### R3d — Hybrid (reason in character + self-evaluate)

**Instruction:** "Reason IN CHARACTER — think as your character would, using your voice, accent, speech pattern, and drives as you think. Then check: did your LAST turn's dialogue reflect your speech pattern and accent? Note any drift and correct it now."

**Hypothesis:** Instead of reviewing attributes analytically, reason *as the character*. The self-evaluation creates a feedback loop — the model sees its own drift assessment next turn and corrects.

**Result (186 events — crashed at tick 14, PULL_ASIDE bug):**

| Marker | Baseline | R3b (passive) | R3d (hybrid)* |
|---|---|---|---|
| Planning | 16.7% | 27.3% | 30.8% |
| Optimism | 24.1% | 13.6% | **23.1%** |
| Third-person | 61.1% | 61.4% | 46.2% |

*Small sample: 26 peter-perfect events.

**Optimism: near-full recovery.** 23.1% — first variant to substantially recover the behavioral disposition. The "reason in character" instruction put the model in the right emotional mode to generate optimistic responses.

**But third-person regressed** to 46.2%. The "reason in character" instruction pulled the model out of the analytical mode needed for objective pattern-checking (is this in third person?).

**Analysis:** Analytical instructions are good for checkable patterns, bad for emotional dispositions. Immersive instructions are the reverse. No single mode wins both.

### R3f — Mental-model-first (plan, review, output)

**Instruction:** "Build your COMPLETE response plan here before writing any other field. Step 1: Review attributes. Step 2: Assess in character. Step 3: Plan dialogue/aside/action. Step 4: Recheck ENTIRE plan against character spec. Step 5: Check last turn for drift. Only then write JSON."

**Hypothesis:** Building a complete mental model before output (like reviewing an academic paper as a whole rather than paragraph by paragraph) should allow multiple in-memory passes that catch both presentational and dispositional drift.

**Result (301 events):**

| Marker | Baseline | R3b (passive) | R3d (hybrid) | R3f (mental-model) |
|---|---|---|---|---|
| Planning | 16.7% | 27.3% | 30.8% | 13.6% |
| Optimism | 24.1% | 13.6% | 23.1% | 9.1% |
| Third-person | 61.1% | 61.4% | 46.2% | **81.8%** |

**Third-person: new high.** 81.8% — exceeded even the baseline with tendencies (61.1%). The structured planning process gave the model maximum analytical control over objective patterns.

**Optimism: worst result.** 9.1% — the multi-step analytical process consumed the thinking budget that immersive reasoning needs for emotional disposition. The model was in "reviewing my plan" mode, not "feeling optimistic" mode.

**Analysis:** Confirms the fundamental tension. More analytical structure → better objective consistency, worse emotional expression. The five-step process is too much structure — it turns character performance into process-following.

### R3g — Minimal evocative (identity activation)

**Instruction:** "Remember who you are — your voice, your way of speaking, your drives. Feel your strongest drive. Think AS your character, not ABOUT your character. If your last turn didn't sound like you, correct it now."

**Hypothesis:** The problem with all previous instructions is mixing analytical and immersive modes. The solution is to abandon analysis entirely and activate identity through evocation. Two sentences, not five steps. Teach a mindset, not a procedure.

The key phrase: "Think AS your character, not ABOUT your character." This explicitly blocks the analytical mode that kills emotional expression while the "remember your voice" phrase activates the presentational patterns.

**Result — initial sample (129 events — crashed at tick 10, PULL_ASIDE bug):**

| Marker | Baseline | R3b (passive) | R3d (hybrid) | R3f (model) | R3g sample* |
|---|---|---|---|---|---|
| Planning | 16.7% | 27.3% | 30.8% | 13.6% | 44.4% |
| Optimism | 24.1% | 13.6% | 23.1% | 9.1% | 22.2% |
| Third-person | 61.1% | 61.4% | 46.2% | 81.8% | 61.1% |

*18 peter-perfect events before PULL_ASIDE crash.

**Full validation run (309 events, 44 Hartwell events):**

| Marker | Baseline | R3g sample (18 evts) | **R3g FULL (44 evts)** |
|---|---|---|---|
| Optimism | 24.1% | 22.2% | **29.5%** |
| Third-person | 61.1% | 61.1% | **88.6%** |

**First variant to exceed baseline on both reliable markers.** Third-person at 88.6% — new all-time high, exceeding R3f's 81.8% ceiling. Optimism at 29.5% — first variant above baseline (24.1%).

**Drift analysis (first half vs second half of Hartwell events):**

| Marker | First half (22 evts) | Second half (22 evts) | Trend |
|---|---|---|---|
| Third-person | 100% | 77.3% | Modest decay, still well above baseline |
| Optimism | 40.9% | 18.2% | Significant decay — halved |

Third-person is self-reinforcing: the model sees its own third-person output in prior turns and perpetuates the pattern. Optimism decays because emotional activation fades as the scenario becomes more action-focused — the evocative instruction primes an emotional state that weakens over time without reinforcement.

**Analysis:** The minimal evocative instruction solves the analytical/immersive tension by not being either. It's identity activation — it primes the model to be the character rather than to think about or review the character. "Feel your strongest drive" puts the model in the right emotional state (optimism recovery). "Remember your voice, your way of speaking" activates presentational patterns (third-person recovery). "Think AS, not ABOUT" prevents the mode-switch that killed one dimension in every structured variant.

**Open question:** The optimism decay (40.9% → 18.2%) suggests that emotional dispositions need periodic re-activation beyond the initial instruction. Presentational patterns (third-person) are self-sustaining because the model's own output reinforces them. Emotional dispositions lack this feedback loop — the model doesn't see "I was optimistic last turn" in the same way it sees "I spoke in third person last turn."

## The Full Comparison

| Run | Instruction type | Events | Optimism | Third-person | Best at |
|---|---|---|---|---|---|
| Baseline | Tendencies present | 375 | 24.1% | 61.1% | — |
| R3 | No instruction | 325 | 4.3% | 13.0% | — |
| R3b | Passive review | 312 | 13.6% | 61.4% | Third-person |
| R3c | Structured fields | 306 | 13.6% | 50.0% | — |
| R3d* | Hybrid (in-character + eval) | 186 | 23.1% | 46.2% | Optimism |
| R3f | Mental-model-first | 301 | 9.1% | 81.8% | Third-person (exceeded baseline) |
| R3g | Minimal evocative | **309** | **29.5%** | **88.6%** | **Both exceeded baseline** |

*Small sample — crashed before full run.

## Design Principles

| # | Principle | Evidence | Round |
|---|---|---|---|
| 1 | Drives carry personality traits | All 7 removals safe | R1 |
| 2 | Drives don't carry storytelling conventions | Scheme narration dropped without encoding | R2 |
| 3 | Drive intensity gates convention-carrying | 0.85 works, 0.7 doesn't | R2b |
| 4 | Emotional-state framing broadens vocabulary | 2x unique emotion words | Emotional-core |
| 5 | Storytelling conventions ≠ personality traits | Asymmetric degradation Wacky Races vs generic | R2 |
| 6 | LLMs prioritise goals over personality | Goal-directed behavior persisted, ambient attributes drifted | R3 |
| 7 | Speech-patterns alone don't override model defaults | Speech-pattern ignored without reinforcement | R3 |
| 8 | Passive review recovers presentational attributes | Third-person fully recovered | R3b |
| 9 | Structured fields create satisficing shortcuts | Model fills blanks then ignores them | R3c |
| 10 | Open-ended prompts outperform structured ones | Show-workings worse than passive review | R3c |
| 11 | Analytical instructions kill emotional expression | Optimism dropped to 9.1% under five-step process | R3f |
| 12 | Immersive instructions recover dispositions | "Reason in character" recovered optimism to 23.1% | R3d |
| 13 | Identity activation avoids the analytical/immersive tradeoff | "Think AS, not ABOUT" recovered both dimensions | R3g |
| 14 | Less instruction can produce better results | Two sentences outperformed five steps | R3g vs R3f |
| 15 | Presentational patterns self-reinforce; emotional dispositions decay | Third-person stable (100→77%), optimism halved (41→18%) over 309 events | R3g full |

## Taxonomy Category Model

| Category | Can emerge from drives? | Where it belongs | Evidence |
|---|---|---|---|
| Personality trait (scheming, gloating) | Yes | Drives | R1: all 7 removals safe |
| Storytelling convention (narrate plans aloud) | No — medium-specific | Drive description at 0.85+ intensity | R2/R2b |
| Voice texture (accent, catchphrases) | Partially | Voice section (style-directive) | Style-directive experiment |
| Internal emotional state | Produces varied external behavior | Drives (emotional-core framing) | Emotional-core: 2x vocabulary |
| Prescribed anchor (evil laugh) | EMERGENCE-GAP — needs neocortex | Voice signature-phrases | Hooded Claw vs Marsh |
| Voice mode (third-person, register shifts) | Yes, with identity activation | Speech-pattern + evocative prompt | R3g: 61.1% |
| Behavioral disposition (optimism, resilience) | Yes, with identity activation | Drives + evocative prompt | R3g: 22.2% |

## Next Steps

### 1. ~~Full validation run for R3g~~ ✓ DONE
Full 309-event run confirmed R3g exceeds baseline on both reliable markers. Third-person 88.6% (new high), optimism 29.5% (first to exceed baseline). PULL_ASIDE fix validated.

### 2. Fix emotional disposition decay
The full run revealed that optimism decays from 40.9% → 18.2% over the second half. Presentational patterns (third-person) self-reinforce via the model's own output; emotional dispositions lack this feedback loop. This is the key problem for longer conversations.

Possible approaches:
- **Engine-level drive reinforcement:** The drive system computes drive states per tick. Surfacing "Your optimistic-determination drive is HIGH" in the observation (not the system prompt) moves the signal into the attended context window. This creates the missing feedback loop for emotional state.
- **Periodic re-activation:** Inject a brief identity reminder into the observation every N ticks (not every turn — that's the R3c trap). The interval should be long enough to avoid satisficing.
- **Emotional state in thinking feedback:** The model sees its own prior thinking next turn. If the thinking field captures emotional state ("I feel optimistic about this"), that creates the same self-reinforcement that third-person enjoys.
- **Combine evocative + passive review:** R3b was best for third-person, R3g for optimism. A hybrid might sustain both if the review clause doesn't trigger the analytical mode. Needs testing.

### 3. Apply to all characters
Apply the evocative instruction universally and compare full-cast behavior across profiles.

### 4. Test over longer runs
With the decay fix in place, test 500+ and 1000+ event scenarios. The question shifts from "does it hold?" to "does the fix create sustainable reinforcement?"

### 5. Build quality tiers
Once the gold-standard instruction (evocative + decay fix) is validated:
- **Tier 1 (gold):** Evocative instruction + engine reinforcement
- **Tier 2 (standard):** Evocative instruction only (current R3g — good for <300 events)
- **Tier 3 (economy):** Passive review only (R3b — good third-person, partial optimism)

## Transcripts

All transcripts in `wacky-manor/docs/eval/`:

| Transcript | Events | Round | Profile | Instruction |
|---|---|---|---|---|
| `new-briefings-20261002/` | 333 | Original baseline | BASELINE | — |
| `generic-colloquial-20261002/` | 310 | Original baseline | GENERIC | — |
| `pareback-r1-baseline-20261003/` | 331 | R1 | BASELINE | — |
| `pareback-r1-generic-20261003/` | 315 | R1 | GENERIC | — |
| `pareback-r2-baseline-20261003/` | 311 | R2 | BASELINE | — |
| `pareback-r2-generic-20261003/` | 334 | R2 | GENERIC | — |
| `pareback-r2b-baseline-20261003/` | 315 | R2b | BASELINE | — |
| `pareback-r2b-generic-20261003/` | 322 | R2b | GENERIC | — |
| `emotional-core-generic-20261003/` | 214 | Emotional-core (truncated) | GENERIC | — |
| `emotional-core-generic-full-20261003/` | 375 | Emotional-core (full) | GENERIC | — |
| `pareback-r3-generic-20261003/` | 325 | R3 | GENERIC | None |
| `pareback-r3b-generic-20261003/` | 312 | R3b | GENERIC | Passive review |
| `pareback-r3c-generic-20261003/` | 306 | R3c | GENERIC | Show-workings |
| `pareback-r3d-generic-20261003/` | 186 | R3d | GENERIC | Hybrid (crashed) |
| `pareback-r3f-generic-20261003/` | 301 | R3f | GENERIC | Mental-model-first |
| `pareback-r3g-generic-20261003/` | 129 | R3g (sample) | GENERIC | Minimal evocative (crashed) |
| `pareback-r3g-full-generic-20261004/` | 309 | R3g (full) | GENERIC | Minimal evocative (validated) |
| `old-briefings-20261002/` | 327 | Pre-rewrite reference | BASELINE | — |
| `generic-briefings-20261002/` | 306 | Prescribed catchphrases reference | GENERIC | — |
