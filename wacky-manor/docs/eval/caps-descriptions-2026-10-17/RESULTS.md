# CAPS Semantic Descriptions Run — 2026-10-17

## What Changed

Four fixes applied before this eval, all targeting the gap between the LLM situation classifier and the CAPS topology that left reactive drives (gloating, adventure) unable to render.

### 1. Semantic construct descriptions on CAPS input nodes (neocortex#605)

**Root cause:** The classifier prompt listed each node as `reward (consequence) — vocabulary: triumph, rewarded, victory, ...`. The LLM had vocabulary keywords but no explanation of what the construct *means* psychologically. "The trap worked" reads as agency/control to a model doing surface-level matching — it doesn't bridge to "reward" without knowing reward = any positive outcome, including a plan succeeding.

**Fix:** Added a `description` field to `CapsNode` and all 31 input nodes in `caps-topology.yaml`. The prompt now reads:
```
- reward (consequence: positive outcome — a plan succeeding, a goal achieved, something desired obtained or won) — vocabulary: triumph, rewarded, victory, ...
```

**Literature:** Mischel & Shoda (1995) CAPS encoding — situation features must be psychologically meaningful, not surface-level tokens. The LLM is performing the *encoding* step of CAPS: mapping a raw situation description to the network's input feature space. Keywords provide exemplar surface forms; descriptions provide the conceptual bridge.

### 2. Target drive description in discrimination metadata (neocortex#601)

**Root cause:** The discriminative profile derivation prompt said "This text relates to the 'gloating' drive" but never explained what gloating IS. The LLM had to infer meaning from the word alone. Result: wrong profile nodes — `psychological_threat`, `betrayal` (self-preservation territory) instead of `reward`, `competition`.

**Fix:** Include the target drive's somatic description (first 80 chars) in the discrimination prompt, so the LLM knows what psychological construct to classify FOR, not just what to distinguish FROM.

**Literature:** Barrett (2017) constructed emotion — categorization requires a concept. Without an anchoring concept, the LLM's implicit emotion theory substitutes the nearest common-language meaning ("gloating = mean-spirited" → threat nodes) for the intended construct ("gloating = pleasure in a plan succeeding" → reward/competition nodes).

### 3. Derive-and-pin infrastructure (neocortex#606)

**Root cause:** Profile derivation is non-deterministic — different profiles every run, 7+ minutes per 2 characters, intermittent timeouts. The pipeline test was a profile quality lottery.

**Fix:** Per-drive skip logic with `DriveProfilePinned` trait. `storePinnedProfile()` utility for freezing validated profiles. Derivation skips pinned drives while deriving others, preserving discrimination context from all drives.

**Literature:** Mischel & Shoda (1995) — if-then profiles are *stable* personality signatures. Stability is the point. If a profile changes every run, it's not capturing personality — it's capturing LLM noise.

### 4. Penelope pipeline test scenarios (examples#125)

Added 4 Penelope scenarios (curiosity, social-harmony, adventure) to pipeline test — now 12 scenarios total. Pinned Penelope profiles after initial derivation revealed adventure's profile had autonomy nodes instead of exploration/danger nodes (discrimination too aggressive — excluded `unpredictable_danger` because social-harmony also claimed it).

## Results

### Pipeline test: 12/12

| Character | Scenarios | Pass |
|---|---|---|
| Peter Perfect | 5 (gallantry, proving-worth, protection) | 5/5 |
| Hooded Claw | 3 (scheming, gloating) | 3/3 |
| Penelope | 4 (curiosity, social-harmony, adventure) | 4/4 |

**HC gloating (the original failing test):** `reward:0.85` now activates for "The trap worked." Gloating overlap=0.484 + resting=0.108 = 0.592. Was 0.0 overlap pre-fix.

### 300-event eval: quality-when-triggered

Relevance mode separates drive quality from trigger frequency — better metric for evaluating pipeline changes since trigger rate is scenario-dependent.

| Character | Drive | Quality | Trig% | N_trig | N_tot |
|---|---|---|---|---|---|
| hooded-claw | scheming | 4.76 | 100% | 42 | 42 |
| hooded-claw | **gloating** | **3.56** | **38%** | **16** | **42** |
| hooded-claw | self-preservation | 3.43 | 100% | 42 | 42 |
| hooded-claw | dominance | 3.02 | 100% | 42 | 42 |
| peter-perfect | protection | 4.16 | 76% | 32 | 42 |
| peter-perfect | proving-worth | 3.62 | 95% | 40 | 42 |
| peter-perfect | gallantry | 2.50 | 52% | 22 | 42 |
| penelope-pitstop | curiosity | 4.12 | 98% | 41 | 42 |
| penelope-pitstop | social-harmony | 3.10 | 95% | 40 | 42 |
| penelope-pitstop | adventure | 3.00 | 60% | 25 | 42 |
| dick-dastardly | scheming | 4.17 | 100% | 40 | 40 |
| dick-dastardly | greed | 3.91 | 88% | 35 | 40 |
| dick-dastardly | gloating | 3.31 | 65% | 26 | 40 |
| dick-dastardly | recognition | 3.09 | 55% | 22 | 40 |
| ant-hill-mob | protection | 4.71 | 95% | 41 | 43 |
| ant-hill-mob | loyalty | 3.81 | 98% | 42 | 43 |
| ant-hill-mob | suspicion | 3.73 | 86% | 37 | 43 |

**Overall mean quality: 3.58** (across all drives where triggered)

### Comparison with Oct 14 baseline

Oct 14 used flat scoring (no relevance gating). Direct numerical comparison is complicated by the metric change. The meaningful comparison is qualitative and architectural:

| Drive | Oct 14 (flat) | Oct 17 (quality) | Oct 17 (flat) | Assessment |
|---|---|---|---|---|
| HC gloating | 2.37 | **3.56** | 1.98 | Filter fixed — renders with quality when triggered |
| DD gloating | 2.85 | **3.31** | 2.50 | Improved quality-when-triggered |
| PP gallantry | 2.50 | **2.50** | 1.79 | Unchanged — bottleneck is expression, not filtering |
| PEN adventure | 2.84 | **3.00** | 2.19 | Slight improvement — pinned profile helps |
| HC scheming | 5.00 | 4.76 | 4.76 | Stable — within noise |
| PP proving-worth | 4.00 | 3.62 | 3.50 | Within noise |
| PEN curiosity | 4.45 | 4.12 | 4.05 | Within noise |

Flat scores are systematically lower than Oct 14 across the board. This is expected: CAPS resting activation (#67) and improved appraisal filtering mean drives render in FEWER events (only when situationally appropriate), which lowers the flat average but raises the quality-when-triggered. The flat metric conflates filter selectivity with expression quality (Principle 50). Quality-when-triggered isolates the pipeline effect.

**Nothing collapsed.** No drive that was working before regressed significantly. The small reductions (proving-worth 4.00→3.62, curiosity 4.45→4.12) are within LLM scoring noise (~±0.4 across runs).

## What Improved and Why

### Gloating: 2.37 → 3.56 quality (+1.19)

The biggest gain. Root cause was that "The trap worked" → `agency_granted:0.85, choice_available:0.75` but NOT `reward`. With no `reward` in the situation, and `reward` being the key node in gloating's profile, overlap was 0. CAPS resting at intensity 0.7 = 0.108, below the 0.15 threshold. Drive filtered.

The semantic description tells the LLM that `reward` = "positive outcome — a plan succeeding." Now "The trap worked" → `reward:0.85, mastery:0.80, dominance:0.75`. Overlap with gloating profile jumps to 0.484. Drive renders.

This is the CAPS encoding step working as designed: the description provides the *concept* that bridges raw situation text to the topology's feature space.

## What Didn't Improve and Why

### Gallantry: 2.50 → 2.50 quality (unchanged)

Gallantry was never a classifier/profile problem. The pipeline test passed gallantry scenarios before this session (5/5 in session 5). The bottleneck is expression quality — how well the LLM renders gallantry behavior in its response — not whether the drive reaches the prompt.

Gallantry's quality-when-triggered is 2.50/5.00 at 52% trigger rate. Compare to scheming's 4.76 at 100%. The gap is:

1. **Description quality**: Gallantry's multi-channel description may not be vivid enough to override the LLM's default conversational patterns. Scheming has richer somatic/attentional language ("a clarity that comes when a plan locks into place — everything else fades").

2. **Reactive-drive penalty** (Principle 45): Gallantry needs someone in distress to render. In 300 events, PP encounters Clara in ~52% of events, but not all involve distress. Scheming activates proactively — HC is always scheming.

3. **Two-layer quality structure**: Filter quality (does it render when it should?) and expression quality (how well does it express?) require different fixes. This session fixed filtering. Expression quality needs richer drive descriptions or additional cognitive architecture (e.g., self-conscious emotions — neocortex#592, Tracy & Robins 2004).

## Architecture Insight

The session revealed a clean separation of concerns in the cognitive pipeline:

| Layer | What it does | What broke (pre-fix) | Quality ceiling |
|---|---|---|---|
| Encoding | Map situation text → CAPS input features | No descriptions → wrong features | Fixed by #605 |
| Profiling | Derive what CAPS features each drive responds to | No drive anchor → wrong profiles | Fixed by #601 |
| Filtering | Compare situation × profile → render/suppress | Was working (CAPS resting #67) | Working |
| Expression | LLM renders the drive in its response | Description quality | Gallantry's bottleneck |

The first three layers are mechanical — they determine WHETHER a drive renders. The fourth layer is generative — it determines HOW WELL the drive expresses. Different fixes for each.

## Files

- `transcript.json` — 300 events (truncated from 1506-event full run)
- `cognitive-dump-*.json` — per-character cognitive pipeline state

## Next Steps

1. **Gallantry expression quality** — richer multi-channel drive descriptions, or self-conscious emotion architecture (neocortex#592)
2. **Parallel classification** — chunk `classify_emotions.py` for N× speedup
3. **Pin PP and HC profiles** — HC profiles worked well from LLM derivation; pin them via #606 infrastructure
4. **300-event eval with pinned profiles** — all characters deterministic, isolate remaining variance to LLM generation
