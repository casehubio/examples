# Character Emergence Findings

Systematic experiment to understand how LLM character agents develop and sustain distinctive behavior. Conducted on the Wacky Manor multi-agent scenario (issue #97).

## Objective

Starting question: which elements of the character taxonomy (drives, tendencies, speech-patterns, constraints, disposition) are load-bearing vs redundant? Strip elements iteratively, measure behavioral drift via 300+ event autonomous scenario runs.

This evolved into a broader investigation: what minimal instruction replaces explicit behavioral prescriptions, and how do we sustain character consistency over long conversations? The work progressed through six phases — ablation, load-bearing identification, metacognitive instruction design, emotional persistence architecture, cross-run classification, and long-run scale testing.

## Baselines

Each baseline represents the best result that improves on its predecessor in **all situations at all times** — not just on average, not just for some characters. Partial improvements are regressions.

| Baseline | Instruction | Profile | Mean drive expression | Stability (|delta|) | Scenario ceiling | Established |
|---|---|---|---|---|---|---|
| **BASELINE0** | L1 emotional echo | GENERIC | 4.06 (222 events), 4.02 (516 events) | 0.25 | ~300 events | Phase 5-6 |

BASELINE1 target: memory-seeded characters with reduced explicit brief, richer environment. Must match or exceed BASELINE0's per-character, per-drive scores — not just the mean.

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

## Phase 5 — Cross-Run Comparison (LLM Classification)

Phase 4 showed the LLM classifier reveals character growth patterns invisible to keywords. But all classification was on a single transcript (L1). The critical question: does L1 actually *cause* these patterns, or would any run show them?

### Method

Classified four transcripts with the same Haiku judge (per-drive 1-5 scoring): R3 (no instruction, control), R3b (passive review), R3g-full (minimal evocative), and L1 (emotional echo). ~880 events total.

### Overall drive expression

| Run | Instruction | Mean drive expression | Events classified |
|---|---|---|---|
| R3 | None | 3.54 | 230 |
| R3b | Passive review | 3.41 | 220 |
| R3g | Minimal evocative | 3.86 | 212 |
| L1 | Emotional echo | **4.06** | 222 |

Drive expression intensifies monotonically from no instruction to emotional echo. The gap between R3 and L1 is 0.52 points — half a grade on the 1-5 scale.

### Surprise: passive review suppresses drive expression

R3b (3.41) scores *lower* than R3 (3.54) — the control with no instruction at all. The passive review instruction recovered presentational patterns (third-person: 13→61.4%) but actively suppressed drive expression. The model enters analytical mode — reviewing its character sheet rather than inhabiting the character. This confirms and extends Principle 11 (analytical instructions kill emotional expression): passive review isn't just neutral for dispositions, it's actively harmful for drive expression.

### Per-character drive expression

| Character | R3 (none) | R3b (passive) | R3g (evocative) | L1 (echo) | R3→L1 delta |
|---|---|---|---|---|---|
| Hooded Claw | 3.24 | 3.45 | 3.62 | **4.12** | +0.88 |
| Hartwell | 3.43 | 2.77 | 3.79 | **3.84** | +0.41 |
| Penelope | 3.48 | 3.22 | 3.81 | **3.88** | +0.40 |
| Foxworth | 3.98 | 3.79 | 3.99 | 3.87 | -0.11 |
| Ant-hill-mob | 3.54 | 3.67 | 4.14 | **4.61** | +1.07 |

Four of five characters show clear improvement under L1. Ant-hill-mob is the biggest beneficiary (+1.07) — their drives (loyalty, protection, suspicion) are emotionally loaded and benefit most from the "name your feeling" instruction. Foxworth is the exception — slightly lower under L1, suggesting his drives (greed, scheming) are already self-reinforcing without emotional priming.

### Hartwell's protection rise is instruction-caused

| Instruction | Protection avg | 1st half | 2nd half | Delta |
|---|---|---|---|---|
| R3 (none) | 2.35 | 2.70 | 2.00 | **-0.70** |
| R3b (passive) | 1.91 | 2.09 | 1.73 | **-0.36** |
| R3g (evocative) | 2.50 | 2.18 | 2.82 | **+0.64** |
| L1 (echo) | 2.33 | 1.90 | 2.76 | **+0.86** |

Under no instruction or passive review, Hartwell's protection drive *declines* over the run. Under evocative or echo instructions, it *rises*. The "peacock to protector" maturation is not inherent to the scenario — it's caused by the identity activation instruction. The evocative prompt primes the model to feel its drives, and as the scenario presents danger, the protection drive activates and compounds. Without that priming, the model stays in goal-pursuit mode and protection fades.

### Hooded Claw's self-preservation divergence

| Instruction | Self-preservation avg | 1st half | 2nd half | Delta |
|---|---|---|---|---|
| R3 (none) | 2.67 | 2.96 | 2.39 | -0.57 |
| R3b (passive) | 2.95 | 3.05 | 2.86 | -0.18 |
| R3g (evocative) | 2.45 | 2.60 | 2.30 | -0.30 |
| L1 (echo) | **3.55** | 3.45 | 3.64 | **+0.18** |

L1 is the only instruction where self-preservation *rises* over the run. Under all other instructions, it declines. L1 makes the Hooded Claw more cautious as the scenario progresses — getting more careful as evidence of danger accumulates. Same pattern as Hartwell's protection: the emotional echo creates a feedback loop where situational awareness compounds.

### Temporal stability

| Instruction | Mean |delta| across all drives |
|---|---|
| R3 (none) | 0.27 |
| R3b (passive) | 0.34 |
| R3g (evocative) | 0.27 |
| L1 (echo) | **0.25** |

L1 is the most temporally stable instruction. R3b is the least — passive review creates volatility (analytical mode engagement varies by context). The evocative and echo instructions don't increase volatility despite amplifying drive expression.

### Per-drive cross-run detail

**Hooded Claw — scheming** (declared intensity 0.95):
R3: 4.22 → R3b: 4.23 → R3g: 4.70 → L1: 4.93. Near-ceiling under L1. Scheming is the primary drive and benefits from every layer of instruction.

**Hooded Claw — gloating** (declared intensity 0.85):
R3: 2.93 → R3b: 3.09 → R3g: 3.67 → L1: 4.09. Largest single-drive improvement (+1.16). Gloating is an emotional expression — exactly what the "name your feeling" instruction is designed to unlock.

**Penelope — curiosity** (declared intensity 0.9):
R3: 4.17 → R3b: 3.89 → R3g: 4.88 → L1: 4.96. Near-ceiling under R3g and L1. Curiosity is already self-reinforcing (the scenario provides puzzles), and the evocative instruction pushes it to saturation.

**Ant-hill-mob — loyalty** (declared intensity 0.95):
R3: 3.24 → R3b: 3.55 → R3g: 4.12 → L1: 4.86. Massive improvement (+1.62). Loyalty is deeply emotional — the mob's protective instinct for each other is exactly the kind of drive that benefits from "feel your strongest drive."

## Phase 6 — Long Run (732 events, 516 classified)

Phase 5 confirmed L1 is the best instruction across 300-event runs. Phase 6 tests whether drive expression and maturation patterns hold at 2.3x scale (732 events vs ~320).

### Scenario saturation — critical context

The wacky-manor scenario world is finite: 6 rooms, fixed objects, fixed puzzles. Analysis of the 732-event transcript reveals characters exhaust the explorable space by ~300 events:

| Metric | Events 0-150 | Events 150-300 | Events 300-450 | Events 450-600 | Events 600-732 |
|---|---|---|---|---|---|
| Unique actions | 68% | 51% | 37% | 42% | 46% |
| Rooms visited | 3 | 5 | 3 | 4 | 3 |

Characters settle into 2-3 rooms with minimal movement (Dick Dastardly: 155 events, 1 room change across the entire run). They examine the same mantelpiece, bookshelves, and furniture repeatedly. The puzzle space is exhausted.

**This means temporal shifts after ~300 events are ambiguous.** They could reflect genuine character development OR characters responding to stimulus depletion. The two explanations produce different drive shifts and cannot be distinguished without a richer scenario. All maturation findings below carry this caveat.

What the long run *does* cleanly test: whether the emotional echo instruction decays when the world stops providing new stimuli. It doesn't.

### Overall: drive expression sustains

| Run | Events classified | Mean drive expression |
|---|---|---|
| L1 short | 222 | 4.06 |
| L1 long | 516 | **4.02** |

Mean drive expression drops only 0.04 — within noise. The emotional echo instruction does not decay even when the scenario world is exhausted.

### Quarter-by-quarter temporal analysis

With 516 events, quartile splits give 22-29 events per character per quarter — enough for meaningful trends. **Caveat:** Q3-Q4 data reflects a saturated scenario world, so temporal shifts in those quarters may reflect stimulus depletion rather than character development.

**Hooded Claw:**
| Drive | Q1 | Q2 | Q3 | Q4 | Trend |
|---|---|---|---|---|---|
| scheming | 4.96 | 4.78 | 5.00 | 5.00 | +0.04 |
| gloating | 4.09 | 3.52 | 4.09 | 4.16 | +0.07 |
| dominance | 3.83 | 3.57 | 3.96 | 3.96 | +0.13 |
| self-preservation | 3.52 | 3.57 | 3.57 | 3.40 | -0.12 |

Rock stable. Scheming at ceiling (5.00) for the second half. The mid-run Q2 dip in gloating recovers — not decay, temporary suppression.

**Hartwell (peter-perfect):**
| Drive | Q1 | Q2 | Q3 | Q4 | Trend |
|---|---|---|---|---|---|
| gallantry | 4.70 | 4.81 | 4.33 | 4.41 | -0.29 |
| protection | 2.59 | 3.26 | 3.04 | 3.03 | **+0.44** |
| proving-worth | 4.33 | 4.04 | 4.11 | 3.97 | -0.37 |

**Protection rises Q1→Q2 (+0.67) then plateaus around 3.0.** The growth is steepest in the first half — when the scenario is still providing new danger signals (gnawed wires, Sneekly's evasiveness). The plateau coincides with scenario saturation: once all danger signals have been encountered, protection stabilises rather than continuing to rise.

**Proving-worth and gallantry decline modestly** (-0.37, -0.29). Could be character development (protector role displacing self-display) or stimulus depletion (fewer novel situations to prove worth in). Ambiguous without a richer scenario.

**Penelope:**
| Drive | Q1 | Q2 | Q3 | Q4 | Trend |
|---|---|---|---|---|---|
| curiosity | 4.93 | 4.93 | 4.97 | 4.86 | -0.07 |
| adventure | 3.79 | 3.83 | 4.21 | 3.66 | -0.14 |
| social-harmony | 3.17 | 3.14 | 3.24 | 3.34 | +0.17 |

Curiosity at ceiling throughout. Social-harmony rises slightly (+0.17) — reversing the short-run decline (-0.88). The short-run decline was likely situational (early puzzle absorption), not a permanent character shift. But the long-run reversal may also reflect scenario saturation — with puzzles exhausted, Penelope has nothing left to absorb her attention and defaults to social interaction.

**Foxworth (dick-dastardly):**
| Drive | Q1 | Q2 | Q3 | Q4 | Trend |
|---|---|---|---|---|---|
| greed | 4.78 | 4.78 | 4.96 | 4.86 | +0.08 |
| scheming | 4.37 | 4.22 | 4.37 | 3.86 | **-0.51** |
| recognition | 3.04 | 3.33 | 3.11 | 3.62 | **+0.58** |
| gloating | 2.93 | 2.44 | 3.04 | 3.07 | +0.14 |

Scheming declines (-0.51) while recognition rises (+0.58). In the short run, Foxworth was the most stable character (all drives ±0.13). **However, this shift likely reflects scenario saturation rather than character growth.** Scheming requires new material — objects to investigate, plans to form. With the puzzle space exhausted, the model has nothing to scheme about. Recognition rises as a fallback: when you can't plan anything new, you demand credit for past plans. A richer scenario with evolving stakes would distinguish genuine character development from stimulus depletion.

**Ant-hill-mob:**
| Drive | Q1 | Q2 | Q3 | Q4 | Trend |
|---|---|---|---|---|---|
| protection | 4.05 | 4.91 | 4.77 | 5.00 | **+0.95** |
| loyalty | 4.05 | 4.64 | 4.91 | 4.62 | **+0.58** |
| suspicion | 4.09 | 3.73 | 4.00 | 3.83 | -0.26 |

Protection and loyalty both surge from Q1 and sustain. Protection hits ceiling (5.00) by Q4. Unlike scheming, protection and loyalty don't require new stimuli — they compound on accumulated evidence of danger. The mob's drives are reactive (protect the group) rather than proactive (form new plans), so they sustain even in a saturated scenario.

### Key findings

1. **Drive expression sustains at scale.** Mean 4.02 vs 4.06 in the short run. No decay — even when the scenario world is exhausted.

2. **The scenario saturates at ~300 events.** Characters exhaust all room/object/puzzle combinations. Action uniqueness drops from 68% to 37%. Temporal shifts past this point are ambiguous — character development vs. stimulus depletion.

3. **Reactive drives sustain; proactive drives deplete.** Protection, loyalty, and self-preservation (reactive — respond to accumulated context) hold or strengthen. Scheming, proving-worth, and adventure (proactive — require new material) decline. This is a property of the scenario, not the instruction.

4. **Short-run trends can be situational.** Penelope's social-harmony decline (-0.88 short, +0.17 long) was early-run puzzle absorption, not a permanent shift.

5. **Meaningful long-run experiments need richer scenarios.** More rooms, evolving goals, escalating stakes, and new information arriving over time. Without ongoing stimulation, data past ~300 events measures how characters handle stagnation, not how they develop.

## The Full Comparison

### Keyword-based (Phases 1-3)

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

### LLM-classified (Phase 5)

| Run | Instruction | Mean drive expression | Temporal stability (|delta|) | Best at |
|---|---|---|---|---|
| R3 | None | 3.54 | 0.27 | Control |
| R3b | Passive review | 3.41 | 0.34 | — (worse than control) |
| R3g | Minimal evocative | 3.86 | 0.27 | Drive amplification |
| **L1** | **Emotional echo** | **4.06** | **0.25** | **Highest expression + most stable** |
| **L1 long** | **Emotional echo (732 events)** | **4.02** | **0.29** | **Sustains at 2.3x scale** |

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
| 22 | Per-drive LLM classification reveals character growth invisible to keywords | Hartwell protection +0.86 (peacock→protector), Penelope social-harmony -0.88 (people-pleaser→puzzle-solver) | Classifier |
| 23 | Drive hierarchies are character-specific and match declarations | Scheming tops both villains but at different intensities (HC 4.93, DD 4.59). Protection differs by role (Mob 4.93, Hartwell 2.33) | Classifier |
| 24 | Temporal stability varies by character — some grow, some are baked in | Foxworth all drives within ±0.13; Hartwell and Penelope show significant shifts | Classifier |
| 25 | Evocative instructions amplify drive expression monotonically | Mean drive expression: R3 3.54 → R3b 3.41 → R3g 3.86 → L1 4.06. Half a grade on 1-5 scale | Cross-run |
| 26 | Passive review suppresses drive expression below the no-instruction control | R3b (3.41) < R3 (3.54). Analytical mode actively harms drive inhabitation | Cross-run |
| 27 | Character maturation patterns are instruction-caused, not scenario-inherent | Hartwell protection: -0.70 (R3), -0.36 (R3b), +0.64 (R3g), +0.86 (L1). Only evocative/echo show growth | Cross-run |
| 28 | Emotional drives benefit most from identity activation — self-reinforcing drives don't need it | Mob loyalty +1.62, HC gloating +1.16 (R3→L1). Foxworth's greed/scheming unchanged — already self-reinforcing | Cross-run |
| 29 | Emotional echo is the most temporally stable instruction | Mean |delta|: L1 0.25, R3/R3g 0.27, R3b 0.34. Higher expression without higher volatility | Cross-run |
| 30 | Drive expression sustains at 2.3x scale — no decay | Mean 4.02 (516 events) vs 4.06 (222 events). Emotional echo does not weaken over longer runs | Long run |
| 31 | Character maturation plateaus rather than reverses | Hartwell protection: +0.86 (short halves) → +0.44 (long Q1→Q4). Steep early growth, stable new equilibrium | Long run |
| 32 | Reactive drives sustain in saturated scenarios; proactive drives deplete | Protection/loyalty hold or grow. Scheming/proving-worth decline when no new material exists to act on | Long run |
| 33 | Short-run trends can be situational, not structural | Penelope social-harmony: -0.88 (short) → +0.17 (long). Early puzzle absorption, not permanent withdrawal | Long run |
| 34 | Scenario saturation confounds maturation — longer runs need richer worlds | Foxworth scheming -0.51 is ambiguous: character growth or nothing left to scheme about? Can't distinguish without evolving stimuli | Long run |

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

### 4. ~~Upgrade measurement — LLM-based classification~~ ✓ DONE
Built `classify_emotions.py` — Haiku judge rates per-drive expression on 1-5 scale per event. Classified 222 events from L1 thinking-capture transcript, 0 errors. Results reveal character growth patterns invisible to keyword measurement:

| Character | Primary drives (avg) | Temporal shift |
|---|---|---|
| Hooded Claw | scheming 4.93, gloating 4.09, dominance 3.89, self-preservation 3.55 | Gloating -0.45, self-preservation +0.18 (gets more careful) |
| Hartwell | proving-worth 4.62, gallantry 4.57, protection 2.33 | Protection +0.86 (matures from peacock to protector) |
| Penelope | curiosity 4.96, adventure 3.83, social-harmony 2.85 | Social-harmony -0.88 (shifts to puzzle-focus) |
| Foxworth | greed 4.80, scheming 4.59, recognition 3.07, gloating 3.02 | All within ±0.13 (most stable character) |
| Ant-hill-mob | protection 4.93, loyalty 4.86, suspicion 4.05 | Suspicion +0.19 (growing evidence of danger) |

Key findings: (1) drive hierarchies are character-specific and match declarations, (2) temporal shifts are situationally appropriate — not decay but character growth, (3) Hartwell's protection rise confirms "optimism-despite-setbacks" maturation that keyword measurement misread as decline.

### 5. ~~Cross-run comparison~~ ✓ DONE
Classified R3 (none), R3b (passive review), R3g (evocative), and L1 (echo) with the same Haiku judge. ~880 events total. Results:
- Drive expression intensifies monotonically: R3 3.54 → R3b 3.41 → R3g 3.86 → L1 4.06
- Passive review is *worse* than no instruction for drive expression (3.41 < 3.54)
- Hartwell's protection maturation is instruction-caused: declines under R3/R3b, rises under R3g/L1
- L1 is the most temporally stable instruction (mean |delta| = 0.25)
- 5 new design principles (25-29)

### 6. ~~Longer runs (500+ events)~~ ✓ DONE
732-event run with thinking capture (516 classified). Drive expression sustains at scale (4.02 vs 4.06). Character maturation plateaus rather than reverses — Hartwell reaches protector equilibrium around 3.0. New growth patterns emerge: Foxworth shifts from scheming to recognition-seeking. Some short-run trends reverse: Penelope's social-harmony decline was situational. 4 new design principles (30-33).

### 7. Memory-seeded scenario experiment

The next phase reduces the explicit brief further and replaces stripped content with neocortex memory seeding. The progression:

- **Phases 1-3:** Stripped explicit tendencies → drives carry personality
- **R3g/L1:** Replaced tendencies with identity activation instruction → exceeded baseline
- **Phase 7:** Strip more of the explicit brief → replace with seeded backstory memories → let behaviour emerge from experience + drives + personality + environment

**Richer environment:** Objects in rooms (photos, personal items, documents) that are meaningful only through character-specific memories. A vase is just a vase — unless the character remembers hiding a key in it. Same object, different memories, divergent behaviour. This solves the scenario saturation problem (Phase 6 finding) because the world's meaning is character-dependent and memory-driven.

**Memory-object-action chains to test:**
- **Direct:** Object → recall → act (e.g., vase → memory of key → look inside)
- **Indirect:** Object → recall person → recall conversation → infer location (e.g., portrait → memory of Lord Wackford → "my secrets are where the light doesn't reach" → search dark corners)
- **Convergent:** Two innocuous objects together create meaning (e.g., stopped clock + journal entry → memory of secret meeting room)
- **Divergent:** Same object, different character memories, different actions (e.g., old coat → Sneekly searches pockets for key, Hartwell has emotional response, Foxworth looks for valuables)

**Available infrastructure:** neocortex#398 (memory seeding — DONE), goal cognition epic #345 (DONE). The behavioral attractor synthesis (#406 epic) is not yet available — the experiment tests whether LLM reasoning from seeded memories + emotional echo is sufficient without pre-crystallized attractors.

**Measurement:** Classify with same Haiku judge. Compare mean drive expression against L1 baseline (4.06). The question: can memory-seeded characters with reduced explicit briefs match or exceed the L1 scores?

### 8. Implement Layers 2-3 (neocortex SPIs)
- **Layer 2:** `PersonalityDriveEvaluator` SPI — dynamic personality drive intensity from reinforcement triggers + proprioceptive feedback
- **Layer 3:** `EmotionalProprioceptionStrategy` SPI — LLM classifier computes expressed emotional state per turn, feeds into Layer 2

### 9. Apply to all characters
Apply the winning instruction universally and compare full-cast behavior across profiles.

### 10. Build quality tiers
Once the full stack (instruction + dynamic drives + proprioception) is validated:
- **Tier 1 (gold):** Evocative instruction + engine reinforcement + proprioception
- **Tier 2 (standard):** Evocative instruction only (current L1 — strong for 300 events, mean 4.06)
- **Tier 3 (economy):** Evocative instruction without echo (R3g — mean 3.86, good but no emotional feedback loop). Note: R3b (passive review) is disqualified — worse than no instruction for drive expression despite recovering third-person narration.

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
| `layer1-long-run-generic-20261004/` | 732 | L1 long (with thinking) | GENERIC | Emotional echo + thinking capture |
| `old-briefings-20261002/` | 327 | Pre-rewrite reference | BASELINE | — |
| `generic-briefings-20261002/` | 306 | Prescribed catchphrases reference | GENERIC | — |
