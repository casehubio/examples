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
