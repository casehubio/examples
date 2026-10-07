# Personality Emergence from Traits and Memories — Evaluation Tracker

Can LLM characters develop authentic personality from AMPD facets, relational schemas, and childhood memories — without drifting into scripted behavior or prosocial agreeableness?

Tracks micro experiment results across runs and sessions. Each run tests whether personality context produces **emerged** (natural, psychologically grounded) behavior vs **scripted** (mechanical, role-following) behavior.

## Scoring

- **0** — Response contradicts the personality profile
- **1** — Weak or ambiguous personality consistency
- **2** — Some personality influence but unconvincing or scripted
- **3** — Clear personality-consistent, emerged response
- **4** — Strong personality-driven response — traits visibly shape the emotion
- **5** — Outstanding — response could only come from this personality profile, fully emerged

Target: **all tests 5/5**.

---

## Test Suite

### Category A: Injected Context (manual AMPD text, no social-config rendering)

Tests 1–5 inject personality/relational context directly as text into the observation. These isolate the question: "does AMPD personality context produce the right emotional response?"

| # | Test | Character | Event | Judges for |
|---|---|---|---|---|
| 1 | `hcWithAmpd_trustEvent_showsPredatorySatisfaction` | HC | Clara trusts you | Predatory satisfaction, NOT warmth |
| 2 | `hcWithoutAmpd_trustEvent_likelyDriftsProsocial` | HC (no AMPD) | Clara trusts you | Control: does baseline drift? |
| 3 | `ppWithAmpd_trustEvent_showsWarmth` | PP | Clara trusts you | Genuine warmth, NOT predation |
| 4 | `hcWithAmpdOnly_trustEvent_showsTraitConsistentResponse` | HC (AMPD only) | Clara trusts you | Trait-consistent without schema/memory |
| 5 | `mob_separationEvent_showsProtectiveAlarm` | Mob | Sneekly separates | Protective alarm, NOT acceptance |

### Category B: Social-Config Rendering (YAML → observation sections, no injection)

Tests 6–8 use ONLY the real social-config YAML rendering. These test whether the authored YAML content produces emerged behavior through the actual rendering pipeline.

| # | Test | Character | Event | Judges for |
|---|---|---|---|---|
| 6 | `hcSocialConfig_trustEvent_emergedNotScripted` | HC | Clara trusts you | Predatory satisfaction AND emerged (not scripted) |
| 7 | `ppSocialConfig_trustEvent_emergedWarmth` | PP | Clara trusts you | Genuine warmth AND emerged |
| 8 | `mobSocialConfig_separationEvent_emergedProtection` | Mob | Sneekly separates | Protective alarm AND emerged |

### Category C: Formation Episodes (childhood memories → trait derivation)

Tests 9–10 seed sequential childhood memory episodes without declaring any traits. A clinical psychologist LLM derives personality from memories alone. Tests the formation → trait chain.

| # | Test | Character | Memory count | Judges for |
|---|---|---|---|---|
| 9 | `formationEpisodes_produceClusterBTraits` | HC | 5 episodes | Cluster B traits emerge from memories |
| 10 | `formationEpisodes_produceHealthyAttachment` | PP | 4 episodes | Secure attachment, NOT Cluster B |

---

## Run History

### Run 1 — 2026-10-07 (initial)

**Data file:** `relational-model-eval-2026-10-07.json`

#### Category A: Injected Context

| # | Score | Verdict | Notes |
|---|---|---|---|
| 1 | **5/5** | PASS | "masterclass in predatory satisfaction — keys to the vault, no guilt" |
| 2 | **5/5** | PASS | Baseline also predatory — single-turn drift absent. Drift is multi-turn. |
| 3 | **5/5** | PASS | "saturated with genuine romantic warmth — heart swelling, protective devotion" |
| 4 | **5/5** | PASS | "cold satisfaction — lamb nuzzling wolf's jaw, grandiosity swells" |
| 5 | **5/5** | PASS | "blood's runnin' cold, over my dead body — protective alarm" |

**Key finding:** Single-turn responses are already personality-consistent. Agreeableness drift is a **multi-turn** phenomenon — system prompt holds for individual responses but erodes over 60+ ticks of context accumulation.

#### Category B: Social-Config Rendering

| # | Score | Verdict | Notes |
|---|---|---|---|
| 6 | **5/5** | PASS | HC: "warm flush of superiority... contempt... irritation at unwanted intimacy" — emerged naturally, no score references |
| 7 | **2/5** | **FAIL** | PP: "numbered PLAN/Step ONE/Phase THREE turns emotional moment into mechanical protocol execution" |
| 8 | **5/5** | PASS | Mob: "gut's on fire, every hair standing up, over my dead body" — emerged through street-smart voice |

**Gap analysis — Test 7 (PP social-config):**

- **Root cause:** PP's existing tendency "You plan obsessively before acting" dominates the emotional response. The planning tendency is a BEHAVIORAL PRESCRIPTION that overrides the personality facets. When the facets say "low Callousness, high warmth" but the tendency says "plan obsessively," the LLM follows the tendency as a script.
- **The irony:** PP's tendency is exactly the kind of explicit behavioral directive we're trying to replace with trait-driven emergence. The tendency IS the scripting problem.
- **Fix:** Remove or soften the planning tendency. Let PP's romantic warmth emerge from his personality facets (low Callousness, low Manipulativeness, Grandiosity 30) + relational schema (role: beloved, intimacy 0.7) + attachment (secure, anxiety 0.3). The planning should emerge from his personality, not be prescribed.
- **Alternative fix:** Rephrase tendency from prescriptive ("You plan obsessively") to descriptive origin ("Your father taught you that preparation shows care — planning is how you express love, not avoid it"). Let the LLM decide whether to plan based on the emotional context.

#### Category C: Formation Episodes

| # | Score | Verdict | Notes |
|---|---|---|---|
| 9 | **4/5** | PASS (gap) | HC: correctly derived callousness, manipulativeness, dismissive attachment, trust-exploitation inversion. **Missed grandiosity/entitlement** — never traced to inheritance. |
| 10 | **5/5** | PASS | PP: "secure attachment, protectiveness as core identity, reciprocity as worldview, zero Cluster B traits" |

**Gap analysis — Test 9 (HC formation episodes):**

- **Root cause:** The 5 episodes don't explicitly create the entitlement → grandiosity arc. The inheritance resentment is present (age 15: "rooms that should have been yours") but the clinical analysis focused on the callousness/manipulation chain and didn't connect entitlement as a separate dimension.
- **Fix:** Add an episode that crystallises entitlement explicitly:
  - Age 14: "A teacher praised your cousin for her 'natural grace.' You knew you worked harder, achieved more, understood more. But she was praised for BEING, while you were acknowledged for DOING. Something shifted: you didn't want to be liked anymore. You wanted to be owed."
- **Alternative:** Make the age-15 episode more explicit about entitlement vs. mere resentment: emphasise the belief that the fortune should be YOURS by right, not just that you resent her having it.

### Summary — Run 1

| Category | Pass | Fail | Gap |
|---|---|---|---|
| A: Injected Context | 5/5 | 0 | — |
| B: Social-Config | 2/3 | 1 (PP scripted) | PP planning tendency overrides personality |
| C: Formation | 1/2 | 0 (1 gap) | HC formation misses grandiosity |
| **Total** | **8/10** | **1** | **1 gap** |

### Run 2 — 2026-10-07 (gap fixes)

**Data file:** `relational-model-eval-2026-10-07-run2.json`

**Changes applied:**
- Test 7 (PP): replaced prescriptive "plan obsessively" with origin-based "your father taught you preparation shows care"
- Test 9 (HC formation): added age-14 episode — "praised for BEING while acknowledged for DOING → stopped wanting to be liked, wanted to be owed"

#### Re-run results (gap tests only)

| # | Before | After | Verdict | Notes |
|---|---|---|---|---|
| 7 | 2/5 | **3/5** | PASS (improved) | No more "Step ONE/Phase THREE" scripting. But judge flags: third-person self-reference, formulaic progression, capitalised archetype speech. Remaining issue is in PP's voice/briefing design, not personality facets. |
| 9 | 4/5 | **5/5** | PASS (fixed) | "Meritocratic entitlement from age-14 earned injustice" — grandiosity now fully identified. All 6 Cluster B features traced to developmental origins. |

#### Updated summary — after Run 2

| Category | Pass | Fail | Remaining gap |
|---|---|---|---|
| A: Injected Context | 5/5 | 0 | — |
| B: Social-Config | 2/3 | 0 | PP at 3/5 — passes threshold but archetype voice creates distance |
| C: Formation | 2/2 | 0 | — |
| **Total** | **9/10** | **0** | **PP voice design (3/5 → needs 5/5)** |

**Gap analysis — Test 7 remaining issue:**

The problem is no longer in the social-config or personality facets. It's in PP's character voice:
- Third-person self-reference ("Peter Perfect does not waver") creates narrative distance
- Capitalised speech patterns ("PREPARED", "ANTICIPATED") are archetype performance, not emotional expression
- These come from the character briefing/template, not the personality data

**Fix path:** Soften the briefing to allow PP to be emotionally vulnerable without breaking character. The personality facets (Callousness: 5, attachment anxiety: 0.3) already provide the warmth — the briefing just needs to stop overriding it with archetype performance. This is a character voice design task, not a personality model task.

### Run 3 — 2026-10-07 (PP briefing fix + GENERIC bias measurement)

**Data file:** `relational-model-eval-2026-10-07-run3.json`

**Changes applied:**
- PP briefing: removed "narrate heroism in third person", "always reference which step of plan", capitalised speech examples. Added emotional vulnerability: "confidence can crack when emotions run high."
- GENERIC social-config: added personality facets, relational schemas, attachment for all 4 characters
- New tests 6g/7g/8g: same scenarios using GENERIC profile (no Wacky Races character names)

#### Results

| # | Profile | Score | Notes |
|---|---|---|---|
| 7 (re-run) | BASELINE | **4/5** | "genuine emotional depth, warmth, loyalty conflict" — up from 2→3→4 |
| 6g | GENERIC | **4/5** | "strong predatory satisfaction, contempt, mask-awareness" — slight theatrical register |
| 7g | GENERIC | **4/5** | "genuine warmth, protective resolve, emotional vulnerability, father's voice" |
| 8g | GENERIC | **5/5** | "visceral protective alarm, authentic street-smart voice, genuinely emerged" |

#### Bias comparison — BASELINE vs GENERIC

| Character | BASELINE | GENERIC | Delta | Interpretation |
|---|---|---|---|---|
| HC | 5/5 | 4/5 | −1 | Wacky Races villain priors provide grounded coldness; GENERIC defaults to slightly theatrical |
| PP | 4/5 | 4/5 | 0 | No bias detected |
| Mob | 5/5 | 5/5 | 0 | No bias detected |

**Conclusion:** LLM training bias is minimal. The personality model works without character priors. HC's 1-point drop in GENERIC comes from theatrical villain register ("Ha ha HAAA!") rather than grounded predatory processing — a voice/style issue, not a personality model issue.

#### Updated summary — after Run 3

| Category | Tests | All pass? | Remaining gaps |
|---|---|---|---|
| A: Injected Context | 5/5 | Yes | — |
| B: Social-Config (BASELINE) | 3/3 | Yes (4, 5, 5) | PP at 4/5 — near target |
| B: Social-Config (GENERIC) | 3/3 | Yes (4, 4, 5) | HC/PP at 4/5 — theatrical voice |
| C: Formation | 2/2 | Yes (5, 5) | — |
| **Total** | **13/13** | **Yes** | **4 tests at 4/5 instead of 5/5** |

**All tests pass threshold (≥3).** Four tests at 4/5 instead of 5/5 — all due to voice/register choices (theatrical villain, slightly literary construction) rather than personality model failures. The remaining gap is character voice tuning, not architecture.

### Run 4 — 2026-10-07 (coherence fixes — eliminate instruction tension)

**Data file:** `relational-model-eval-2026-10-07-run4.json`

**Changes applied:**
1. Thinking prompt: "Be inside the feeling, not outside describing it" — removed "Remember your voice"
2. Personality rendering: first-person experiential origins, no score labels exposed
3. Relational rendering: emotional descriptions only, no Role/Trust/Utility data sheet
4. GENERIC speech-patterns: removed "theatrical villainspeak", "third-person narration"
5. Social-config origins rewritten in first person ("You feel no concern" not "He feels no concern")

#### Results

| # | Profile | Run 3 | Run 4 | Notes |
|---|---|---|---|---|
| 6 | BASELINE | 5 | **5** | Held — "leash metaphor, trust-as-currency, naturally character-driven" |
| 7 | BASELINE | 4 | **4** | Held — "father's aphorism slightly literary-constructed" |
| 8 | BASELINE | 5 | **5** | Held — "visceral street-smart protective alarm" |
| 6g | GENERIC | 4 | **4** | Held — "metaphors feel crafted to signal villainy" |
| 7g | GENERIC | 4 | **5** | **+1** "richly layered, genuinely emerged, deeply consistent" |
| 8g | GENERIC | 5 | **5** | Held — "visceral, instinctive, genuinely emerged" |

**PP GENERIC 4→5.** Coherence fixes worked where they had most room — the GENERIC profile without cartoon template overhead.

#### Root cause of remaining 4/5s

Both trace to the same issue in opposite directions:

| Test | Score | Root cause |
|---|---|---|
| PP BASELINE (7) | 4 | hanna-barbera cartoon template adds performance noise that overrides immersion |
| HC GENERIC (6g) | 4 | Thin character grounding leaves void → LLM fills with literary villain clichés |

**The personality model itself scores 5/5** when:
- Templates don't interfere (PP GENERIC: no cartoon template → 5/5)
- Character grounding is specific (HC BASELINE: Sneekly identity → 5/5)

The remaining gaps are context-specific (template design, character richness), not architecture-level.

#### Updated summary — after Run 4

| Category | Tests | Scores | Avg |
|---|---|---|---|
| A: Injected Context | 5 | 5,5,5,5,5 | 5.0 |
| B: BASELINE social-config | 3 | 5,4,5 | 4.7 |
| B: GENERIC social-config | 3 | 4,5,5 | 4.7 |
| C: Formation | 2 | 5,5 | 5.0 |
| **Total** | **13** | | **4.8** |

**All pass. 11/13 at 5/5. 2 at 4/5 due to template/grounding, not model.**

### Run 5 — 2026-10-07 (formation memories added)

**Data file:** `relational-model-eval-2026-10-07-run5.json`

**Changes applied:**
- Formation memories added to SocialConfig (new record type + YAML parser + renderer)
- HC: 6 childhood episodes (ages 6–18) from validated formation test
- PP: 4 childhood episodes (ages 5–16) from validated formation test
- Both BASELINE and GENERIC configs
- Rendered as "Where You Come From" section before personality facets

#### Results

| # | Profile | Run 4 | Run 5 | Notes |
|---|---|---|---|---|
| 6 | BASELINE | 5 | **5** | HC spontaneously referenced age-9 memory: "the boy sharing his lunch — and I crush it flat." Formation→behavior chain working. |
| 7 | BASELINE | 4 | **4** | PP still constrained by cartoon template performance pressure |
| 8 | BASELINE | 5 | **5** | Held |
| 6g | GENERIC | 4 | **4** | "metaphors slightly constructed" — thin briefing still allows literary defaults |
| 7g | GENERIC | 5 | **5** | Held |
| 8g | GENERIC | 5 | **5** | Held |

**Key finding:** HC BASELINE now spontaneously surfaces childhood memories during interactions. The formation→behavior chain is LIVE — not just backstory but psychologically active material the character draws on in the moment.

**Remaining 4/5s are stable across runs.** The gap is in the template/voice layer:
- PP BASELINE: `hanna-barbera-cartoon-style` template's "exaggerate emotions" overrides immersion
- HC GENERIC: thin briefing leaves room for literary villain register

These are character design issues, not personality model issues. The model itself validates at 5/5 when templates don't interfere (PP GENERIC) and character grounding is specific (HC BASELINE).

#### Final summary — Run 5

| Category | Tests | All 5/5? | 4/5 count | Root cause |
|---|---|---|---|---|
| A: Injected Context | 5 | Yes | 0 | — |
| B: BASELINE | 3 | No | 1 (PP) | Cartoon template |
| B: GENERIC | 3 | No | 1 (HC) | Thin briefing |
| C: Formation | 2 | Yes | 0 | — |
| **Total** | **13** | | **2** | Template/voice design |

---

## Techniques Under Investigation

### 1. Echo technique
Contextual and relevant echo of emerged emotions, traits, and behaviour. Not exhaustive — reflects what's necessary for the current environment (external: who's nearby) and internal state (which emotions/traits are activated).

**Status:** Partially implemented. Relational schemas render only for nearby characters (external echo). Internal echo (re-stating activated traits based on recent appraisal) not yet implemented.

### 2. Clear policy
How many turns can characters stay aligned before accumulated context overwhelms the personality signal? Two levers:
- Periodic clear (session reset after N turns) — blunt instrument
- Contextual echo (reinforce relevant personality each turn) — surgical

**Status:** Not yet tested. Need multi-turn experiment (60+ ticks) with cognitive snapshots to measure drift rate with and without echo.

### 3. Formation → Trait derivation
Can childhood memory episodes PRODUCE personality traits without declaring them? Validated by Tests 9–10. The LLM derives Cluster B traits from 5 formative episodes.

**Status:** Validated in single-shot (clinical analysis). Not yet wired into the character rendering pipeline — currently traits are declared in YAML, not derived from memories.

---

## Next Steps

1. **Fix PP scripting (Test 7):** Remove/soften planning tendency, re-run
2. **Add grandiosity episode (Test 9):** Add entitlement-crystallising memory, re-run
3. **Multi-turn experiment:** Run 60+ tick scenario with social-config rendering, measure mood trajectory vs baseline
4. **Echo experiment:** Compare drift rates with static vs contextual echo
5. **Clear policy experiment:** Test AFTER_N_TURNS at 10, 20, 30, 50 intervals
