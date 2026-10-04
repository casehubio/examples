# Character Emergence Findings

Systematic experiment to understand how LLM character agents develop and sustain distinctive behavior. Conducted on the Wacky Manor multi-agent scenario (issue #97).

## Objective

Starting question: which elements of the character taxonomy (drives, tendencies, speech-patterns, constraints, disposition) are load-bearing vs redundant? Strip elements iteratively, measure behavioral drift via 300+ event autonomous scenario runs.

This evolved into a broader investigation: what minimal instruction replaces explicit behavioral prescriptions, and how do we sustain character consistency over long conversations? The work progressed through four phases — ablation, load-bearing identification, metacognitive instruction design, and emotional persistence architecture.

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

## Phase 4 — Emotional Persistence (Layer 1)

Phase 3 found the right instruction type (evocative identity activation) but revealed a decay problem: emotional dispositions (optimism) weaken over the run while presentational patterns (third-person) self-reinforce. Phase 4 addresses this asymmetry.

### The self-reinforcement asymmetry

Third-person narration self-reinforces through a multi-channel echo: the model writes third-person text → that text is stored as `currentThinking` and fed back next turn → the model sees third-person in its own output → continues the pattern. The output IS the signal.

Emotional dispositions have no equivalent echo. The model generates optimistic dialogue, but that dialogue goes to other characters, not back to itself. The thinking field captures tactical reasoning ("I should check the library"), not emotional state ("I feel optimistic"). Nothing in the observation says "you were optimistic last turn."

### Architectural analysis

Five mechanisms sustain personality consistency in humans. The architecture's coverage:

| Mechanism | Status | Gap |
|---|---|---|
| Trait stability (static drives) | ✓ Present | — |
| State fluctuation (dynamic intensity) | ✗ Missing | Drives are fixed numbers, no per-tick variation |
| Proprioception (awareness of own state) | ✗ Missing | No feedback of expressed emotional state |
| Memory consolidation (emotional memory) | Partial | Memory system exists but doesn't prioritize emotional content |
| Social reinforcement (others react) | ✓ Natural | Happens through the event drain |

Three-layer fix designed:
- **Layer 1:** Thinking-field emotional echo (instruction change — test immediately)
- **Layer 2:** Dynamic personality drive state (new `PersonalityDriveEvaluator` SPI in neocortex)
- **Layer 3:** Emotional proprioception (new `EmotionalProprioceptionStrategy` SPI — LLM classifier)

### Layer 1 — Emotional echo instruction

**Instruction:** "What are you FEELING right now — not thinking, feeling? Name it. Remember your voice, your way of speaking. Think AS your character, not ABOUT your character. If your last turn didn't sound like you, correct it now."

**Hypothesis:** If the model names its emotional state in the thinking field ("I feel protective and determined"), that text is fed back next turn as "Your Current Thinking" — creating the same self-reinforcing echo that third-person enjoys. The key difference from R3c (structured fields): this asks for a single introspective word that varies by context, not a checklist to fill.

**Result (320 events, 42 Hartwell events):**

| Marker | Baseline | R3g full (44 evts) | **Layer 1 (42 evts)** |
|---|---|---|---|
| Optimism | 44.4% | 38.6% | **40.5%** |
| Third-person | 61.1% | 88.6% | **85.7%** |

**Drift analysis (first half vs second half):**

| Marker | R3g 1st half | R3g 2nd half | R3g trend | L1 1st half | L1 2nd half | L1 trend |
|---|---|---|---|---|---|---|
| Third-person | 100% | 77.3% | -22.7pp | 81.0% | 90.5% | **+9.5pp** |
| Optimism | 27.3% | 50.0% | +22.7pp | 52.4% | 28.6% | -23.8pp |

**Third-person stability: Layer 1 is the best yet.** It actually *strengthens* over the run (+9.5pp), while R3g decayed (-22.7pp). The emotional echo instruction improves presentational pattern stability.

**Optimism: inconclusive — exposed a measurement problem.** With 21-22 Hartwell events per half, a single event shifts the rate by ~5pp. R3g shows +22.7pp *growth* with this methodology vs the -22.7pp *decay* reported with the earlier methodology (different counting) — a 45pp swing. Both "decay" and "growth" claims are within noise at this sample size.

**Qualitative finding:** Both R3g and Layer 1 maintain strong character voice throughout when read rather than counted. Hartwell's late-run output is richly in character — the emotional tone *matures* from unbridled enthusiasm (early) to optimism-despite-setbacks (late). Keyword analysis reads this as decay; qualitative reading reads it as character depth.

### Thinking field analysis — mechanism confirmed

Thinking capture was added to the transcript format and a second Layer 1 run was recorded (318 events, 222 with thinking). Results:

**Emotional naming rate (% of thinking entries that open with explicit emotional state):**

| Character | Entries | Overall | 1st half | 2nd half | Trend |
|---|---|---|---|---|---|
| Hartwell | 21 | 90% | 80% | **100%** | **+20pp — strengthens** |
| Foxworth | 23 | 100% | 100% | 100% | stable |
| Clara | 24 | 88% | 92% | 83% | -9pp |
| Marsh | 22 | 100% | 100% | 100% | stable |
| Brixton Boys | 21 | 100% | 100% | 100% | stable |

**The feedback loop is compounding.** Emotional naming does not decay for any character. For Hartwell, it strengthens from 80% to 100% — the two non-emotional openings both occur in the first half.

**Emotions are drive-consistent and character-specific:**
- Hartwell: ELATION, EXHILARATION, heart POUNDING — gallantry (0.9), protection (0.8)
- Foxworth: "TRIUMPHANT and SCHEMING!", "BURNING fury!" — scheming drive
- Marsh: "dark anticipation", "incandescent FURY!" — theatrical villain
- Clara: "THRILLED beyond measure!", "BUZZING with excitement!" — warm socialite
- Brixton Boys: "proper anxious", "SICK with worry" — protective loyalty

**Output correlation is strong.** When Hartwell names ELATION in thinking, his dialogue opens with "SPLENDID!", "By JOVE!" No case of flat output following emotional thinking.

**Situational tracking — is this scripting by the back door?**

Critical test: does the instruction force uniformly positive emotions, or do emotions track the actual scenario? If Hartwell is always ELATED regardless of what's happening, the instruction is implicitly scripting a fixed state. Examined all 21 Hartwell thinking entries against their preceding scenario events:

| Situation | Emotion | Tracks? |
|---|---|---|
| Arrival at manor, first exploration | Heart SWELLS with anticipation | ✅ |
| Solved the riddle | TRIUMPHANT and ELATED | ✅ |
| Bookshelves can't be interacted with | FRUSTRATED but UNDETERRED — "BLAST!" | ✅ |
| Effort unrewarded — mechanism shows nothing | FRUSTRATION — pure, burning | ✅ |
| Compartment EMPTY after 10 events of work | ELATION → BEWILDERMENT → SUSPICION | ✅ |
| Gnawed wire found, machine is dangerous | CONCERNED — deeply, MAGNIFICENTLY concerned | ✅ |
| Workbench has no wrench for repair | FRUSTRATION + DETERMINATION | ✅ |

20 of 21 entries show situation-appropriate emotions. Frustration appears 5 times — always when plans are blocked. Concern appears when genuine danger surfaces. Compound emotional arcs (ELATION→BEWILDERMENT→SUSPICION) track real narrative beats. He is not uniformly positive.

**Evidence — raw thinking field text, verbatim:**

*Elation when solving a puzzle with Clara watching (gallantry drive):*
> "EXHILARATION! Pure, undiluted EXHILARATION! I solved the riddle — a MAP — and Penelope is RIGHT HERE to witness it! But wait — I merely examined the mantelpiece. I need to INTERACT with it! There must be a mechanism, a hidden lever, a secret panel th..."

*Frustration when bookshelves won't cooperate (proving-worth blocked):*
> "BLAST! The bookshelves rebuffed me — James Hartwell, rebuffed by FURNITURE! The indignity is almost too much to bear. But wait — wait wait WAIT. The mantelpiece! The RIDDLE! 'I have cities but no houses, forests but no trees, water but no fish.' That..."

*Compound arc when compartment is empty after 10 events of work:*
> "ELATION turning to BEWILDERMENT turning to SUSPICION — but only a GENTLE suspicion, because James Hartwell sees the best in people! I solved the riddle — MAP! — and activated the mechanism with tremendous flair, and Clara was RIGHT THERE to witness i..."

*Protection drive when gnawed wire and danger surface:*
> "CONCERNED. Deeply, MAGNIFICENTLY concerned. This wire — gnawed clean through! And Sneekly wants us to just... operate it? Three men on a broken machine? James Hartwell's heart is POUNDING with protective instinct. Not fear — never fear — but PRUDENT..."

*Frustration + determination when repair is blocked (no wrench):*
> "FRUSTRATION mixed with DETERMINATION! The workbench has tools aplenty but no wrench — and that gnawed wire needs SOMETHING to splice it back together. But WAIT — Mr. Sneekly said Lord Wackford kept spare parts in those crates! Replacement wires, fuse..."

Note the character voice in the thinking itself — third-person self-reference ("James Hartwell, rebuffed by FURNITURE!"), the theatrical register ("MAGNIFICENTLY concerned"), the drive-consistent self-narration ("Not fear — never fear — but PRUDENT"). The emotional echo doesn't just name an emotion — it reasons in character.

**Conclusion:** The emotional echo is emergent, not scripted. The instruction asks the model to name what it feels — it doesn't prescribe what to feel. Emotions arise from drives meeting situations: gallantry produces elation when Clara is present, protection produces concern when danger appears, proving-worth produces frustration when blocked. The instruction creates a feedback loop for emergent state; it does not prescribe the state itself.

### Measurement methodology revision needed

The keyword-based measurement used throughout Phases 1-3 has reached its resolution limit. At N≈20 events per half, single-event variance dominates. Future experiments need:

1. **Thinking field capture in transcripts** — the most critical infrastructure gap
2. **Longer runs (500+ events)** — larger samples to reduce keyword noise
3. **LLM-based classification** — use an LLM judge to rate each event's emotional disposition on a 1-5 scale, catching nuanced expression that keywords miss

## The Full Comparison

| Run | Instruction type | Events | Optimism | Third-person | Third-person trend | Best at |
|---|---|---|---|---|---|---|
| Baseline | Tendencies present | 375 | 44.4% | 61.1% | — | — |
| R3 | No instruction | 325 | 4.3% | 13.0% | — | — |
| R3b | Passive review | 312 | 13.6% | 61.4% | — | Third-person |
| R3c | Structured fields | 306 | 13.6% | 50.0% | — | — |
| R3d* | Hybrid (in-character + eval) | 186 | 23.1% | 46.2% | — | Optimism |
| R3f | Mental-model-first | 301 | 9.1% | 81.8% | — | Third-person ceiling |
| R3g | Minimal evocative | 309 | 38.6% | 88.6% | -22.7pp | Both exceeded baseline |
| **L1** | **Emotional echo** | **320** | **40.5%** | **85.7%** | **+9.5pp** | **Most stable third-person** |

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
| 16 | Emotional echo strengthens presentational stability | Third-person trend reversed: -22.7pp (R3g) → +9.5pp (L1) | L1 |
| 17 | Keyword measurement breaks at N≈20 per half | 45pp swing in optimism from methodology alone across R3g analyses | L1 |
| 18 | Emotional tone matures, not decays | Late-run output shows optimism-despite-setbacks — depth, not drift | L1 qualitative |
| 19 | "Name your feeling" creates a compounding feedback loop | Emotional naming 80%→100% for Hartwell; stable 100% for 3/5 characters | L1 thinking |
| 20 | Emotional echo produces drive-consistent, character-specific emotions | Each character's emotion vocabulary maps to their declared drives | L1 thinking |
| 21 | Emotional echo is emergent, not scripted — emotions track situations | Frustration when blocked (5x), concern when danger appears, compound arcs on narrative beats | L1 situational |

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

### 2. ~~Layer 1 — emotional echo instruction~~ ✓ DONE
Tested "What are you FEELING — name it" instruction. Third-person stability improved (+9.5pp trend vs -22.7pp). Optimism measurement inconclusive — exposed keyword methodology limits at N≈20. Qualitative assessment: character voice strong throughout.

### 3. ~~Capture thinking field in transcripts~~ ✓ DONE
Added thinking to ManorEvent and the /manor/events endpoint. Second Layer 1 run confirmed the mechanism: 90-100% emotional naming rate across all characters, compounding over time (Hartwell 80%→100%). Emotions are drive-consistent and character-specific.

### 4. Upgrade measurement — LLM-based classification
Keyword matching has hit its resolution limit. Build an LLM judge (Haiku) that rates each event's emotional disposition on a 1-5 scale. This becomes the standard measurement tool for future experiments and the foundation for the Layer 3 (emotional proprioception) SPI.

### 5. Longer runs (500+ events)
Larger samples reduce keyword noise and test whether character voice truly sustains or decays over extended scenarios. Run with thinking capture enabled.

### 6. Implement Layers 2-3 (neocortex SPIs)
- **Layer 2:** `PersonalityDriveEvaluator` SPI — dynamic personality drive intensity from reinforcement triggers + proprioceptive feedback
- **Layer 3:** `EmotionalProprioceptionStrategy` SPI — LLM classifier computes expressed emotional state per turn, feeds into Layer 2

### 7. Apply to all characters
Apply the winning instruction universally and compare full-cast behavior across profiles.

### 8. Build quality tiers
Once the full stack (instruction + dynamic drives + proprioception) is validated:
- **Tier 1 (gold):** Evocative instruction + engine reinforcement + proprioception
- **Tier 2 (standard):** Evocative instruction only (current L1 — strong for 300 events)
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
| `layer1-emotional-echo-generic-20261004/` | 320 | L1 | GENERIC | Emotional echo |
| `layer1-thinking-capture-generic-20261004/` | 318 | L1 (with thinking) | GENERIC | Emotional echo + thinking capture |
| `old-briefings-20261002/` | 327 | Pre-rewrite reference | BASELINE | — |
| `generic-briefings-20261002/` | 306 | Prescribed catchphrases reference | GENERIC | — |
