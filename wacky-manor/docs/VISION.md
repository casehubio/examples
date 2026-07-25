# Wacky Manor — Vision

> Blue sky ideas, brainstorming, and everything we think this could become.
> Not a spec. Not a plan. A reference for what we imagined and why.

## The Idea

Take characters from Wacky Races (and The Perils of Penelope Pitstop), give
each one a personality via CaseHub Eidos descriptors, drop them into a haunted
mansion, give them secret goals, and let LLM agents improvise all dialogue
and reasoning in character. The overall arc is scripted — we know how it
unfolds — but the LLMs don't know the game mechanics. They experience it in
real time.

The result: a playable, watchable, interactive demo that exercises CaseHub's
agent identity, communication, orchestration, and summarisation infrastructure
— while being genuinely fun.

Target audience: developers.

## Why This Exists

CaseHub has rich infrastructure for multi-agent coordination — Eidos for
identity, Qhorus for communication, Engine for orchestration, Blocks for
agentic patterns and summarisation. But no demo that makes this visible and
entertaining. The existing examples are technical showcases. This is a
showcase that people want to watch.

The demo is also a requirements driver. Where CaseHub lacks a capability the
demo needs, that's a legitimate platform enhancement.

## The Genre

A hybrid story/game. Like a point-and-click adventure (Maniac Mansion, Sam &
Max Hit the Road) with LLM actors. The game designer scripts the world, the
puzzles, the character abilities, and the key plot beats. The LLMs provide
the dialogue, the reasoning, the personality, and the moment-to-moment
interactions.

Some interactions are purely contrived comedy devices — a character or object
exists in a room because triggering the interaction is funny, not because it
serves the plot. Sam & Max is the reference: the world-building serves the
jokes, not the other way around.

## Scenario Premise

Each character has received a mysterious invitation:

> "You are cordially invited to Doily Manor for a most EXTRAORDINARY
> evening. Survive the night, solve the mansion's puzzles, and the last
> one standing wins ONE MILLION DOLLARS. But beware — the mansion has a
> mind of its own..."

This framing gives every character a reason to be there, a reason to
cooperate (some puzzles need multiple people), and a reason to compete
(only one winner). It also provides:

- **Elimination justification** — voted out characters "didn't survive
  the night"
- **Hooded Claw's cover story** — he's there for the money, like everyone
  else (or so they think). His real mission is Penelope, but the
  competition gives him a plausible reason to be in every room.
- **Character-specific motivations layered on top:**
  - Penelope wants to solve the puzzles — the money is nice but the
    adventure is the real draw
  - Dastardly wants the million and will cheat for it
  - Peter Perfect wants to win so he can give the money to Penelope
  - Pat Pending doesn't care about the money — the mechanisms are the prize
  - Ant Hill Mob are there because Penelope is there
  - Hooded Claw uses the competition as cover for his real mission

## The Setting: Doily Manor

A haunted Victorian mansion. Multiple floors, interconnected rooms.

### Visual Style

Door Kickers: Action Squad "ant farm" cutaway cross-section. The entire
mansion is visible at once as a 2D side-view. Characters are visible in their
rooms. Movement is primarily left/right through connected rooms on the same
floor. Y only changes at stairs or doors between floors.

```
Floor 2:  [  Tower  ]
          [  (locked)  ]
          ─────┬────────────────────────
               │ stairs
Floor 1:  [ Ballroom ] ←→ [ Library ] ←→ [   Lab   ]
          ─────┬──────────────┬──────────────┬──────
               │ stairs       │ stairs       │ stairs
Floor 0:  [ Kitchen  ] ←→ [ Entrance ] ←→ [ Cellar  ]
                           [   Hall   ]
```

Characters move left/right through rooms. Stairs connect floors vertically.
You see every room at once. When something happens in the Kitchen, you see
the character icons there. When the narrator describes the Library, you
glance at that room. The whole mansion is always visible.

### UI Layout

```
┌──────────────────────────────────────────────────┐
│              DOILY MANOR (cross-section)          │
│  Characters visible as icons in their rooms.     │
│  Items visible on shelves and tables.            │
│  Characters move left/right, up/down at stairs.  │
├─────────────────────────┬────────────────────────┤
│  Dialogue               │  Narrator              │
│  (Qhorus work channel)  │  (Qhorus observe)      │
│                         │                        │
│  Character speech in     │  Breathless omniscient  │
│  colored bubbles per     │  Wacky Races narrator   │
│  character. Room label.  │  style. Villain asides  │
│                         │  in dark red.           │
└─────────────────────────┴────────────────────────┘
```

## The Cast

### Tier 1 — Full Agents (have goals, make decisions, navigate the mansion)

**Penelope Pitstop**
Sweet-natured Southern belle. Far more capable than anyone gives her credit
for. Trusts people too easily. Oblivious to personal danger. "Why, how
delightful!" / "Hayulp! Hayulp!"
- Goal: Find the Doily Diamond.
- Doesn't know: The Hooded Claw is trying to kill her.

**The Hooded Claw (disguised as Sylvester Sneekly)**
Penelope's secret nemesis, disguised as the estate manager. Two voices:
Sneekly (obsequious, helpful) and villain monologues (grandiose, theatrical,
"Nyah-ha-ha-HA!"). Must appear helpful while secretly sabotaging.
- Goal: Kill Penelope before she finds the treasure.
- Capability: Can discover hidden dangerous items others can't see.
- Doesn't know: What devices are where. Discovers them room by room.

**The Ant Hill Mob (Clyde as spokesperson)**
Seven small-time gangsters, secretly Penelope's devoted protectors. Gruff
Brooklyn gangster voice. Bumbling but loyal. Deeply suspicious of Sneekly.
Save Penelope by accident rather than skill.
- Goal: Protect Penelope from all dangers.
- Capability: Can notice suspicious behavior near Penelope.

**Peter Perfect**
Handsome, gallant, hopelessly devoted to impressing Penelope. Volunteers for
every dangerous task. Makes things worse before accidentally making them
better. Narrates his own heroism in the third person when excited.
- Goal: Impress Penelope. Be heroic.

**Professor Pat Pending**
Eccentric inventor, fascinated by mechanisms. Speaks in technical terms that
confuse everyone. Only character who can operate complex machinery. Oblivious
to social dynamics — doesn't notice the scheming or the romance.
- Goal: Understand and operate every mechanism in the mansion.

### Tier 2 — Roaming NPCs (wander around, create problems by being themselves)

**Dick Dastardly & Muttley**
Dastardly is the self-appointed mansion guide. Lies about everything — wrong
directions, fake combinations, misleading clues. Wants the treasure for
himself. Schemes always backfire. "Drat, drat, and double DRAT!"

Muttley doesn't speak in full sentences. Snickers ("Hehehehehehe!"), grumbles
("Rassafrassa..."), sniffs things. Has an excellent nose — alerts others to
danger inadvertently. Wants medals.

**Sergeant Blast & Private Meekly**
Blast has "secured" a corridor and won't let anyone through without the
password. There is no password. He made it up. Meekly apologetically waves
people through when Blast isn't looking.

### Tier 3 — Static Room Fixtures (planted in a room, exist to trigger a gag)

**The Slag Brothers (Rock & Gravel)** — Library
Trying to read a book. It's upside down. Ask them about the riddle:
"Slag?" *smashes the mantelpiece* — accidentally reveals a hidden compartment.

**Lazy Luke & Blubber Bear** — Ballroom
Asleep. Luke is snoring in a chair with a secret lever underneath. Blubber
is using the treasure map as a blanket. To get the map, don't wake Blubber —
he'll panic-destroy the room if startled.

**The Gruesome Twosome (Big Gruesome & Little Gruesome)** — Cellar
Think the mansion is lovely. Redecorating. Little Gruesome has explored every
air vent and knows the layout but communicates only in squeaks. Big Gruesome
is wearing the missing spring from the Lab machine as a bracelet because
"it's pretty."

**Rufus Ruffcut & Sawtooth** — Laboratory
Fixing a broken chair. Sawtooth has gnawed through a crucial wire on the
machine. Rufus can fix it, but only with "a proper wrench, not these fancy
city tools." The wrench is in the Kitchen.

## Cartoon Character Communication: Expository Soliloquy

Cartoon characters — especially Hanna-Barbera characters — don't just talk
to each other. They narrate their situation aloud. This is called
**expository soliloquy** (or **performative self-narration**). It's distinct
from narration, internal monologue, and normal dialogue. It's a fourth mode
where the character verbalizes their situation, emotions, and intentions
aloud — technically to themselves or whoever's nearby, but really for the
audience.

**Penelope (victim exposition):** "Oh my! That dreadful Hooded Claw has
tied me to this here conveyor belt, and it's headin' straight for that big
ol' buzzsaw! Hayulp! Hayulp! Won't someone please save little ol' me?"

**Hooded Claw (villain plan narration):** "And NOW, when dear Penelope
steps on THAT tile, it shall trigger THIS trapdoor, sending her plummeting
into the crocodile pit below! NOTHING can save her now! Nyah-ha-ha-HA!"

**Dastardly (frustrated recap):** "Drat! Double drat! Those goody-two-shoes
have won AGAIN! Come, Muttley, we must devise an even MORE dastardly plan!"

This convention is critical for the demo. Without it, the characters will
either be silent (just taking actions) or have normal conversations.
Neither feels like a cartoon. Every character briefing must explicitly
instruct this mode:

> You frequently narrate your situation aloud — describing what's
> happening to you, how you feel about it, and what you intend to do
> next. You do this in your character voice, often to yourself, to nearby
> characters, or to no one in particular. When in danger, describe the
> danger. When scheming, explain your scheme. When frustrated, enumerate
> your frustrations. You repeat and reinforce key plot points through
> your dialogue.

This also solves a practical problem: **it makes the transcript self-
documenting.** The audience doesn't need to read game state — the
characters TELL you what's happening through their performative self-
narration. The narrator adds dramatic flair on top, but the characters
themselves carry the exposition.

**Phase 0 must validate this.** If characters don't do expository soliloquy
naturally from the descriptor, the whole demo will feel like LLM chatbots
in costume rather than cartoon characters.

## Eidos Enhancement: Libraries and Templates

The Wacky Manor demo surfaces a gap in Eidos's descriptor model. Currently,
behavioral instructions live in a single `briefing` text field per
descriptor. But cartoon characters share genre-level conventions (expository
soliloquy, emotional telegraphing, catchphrase repetition) and role-level
conventions (villain monologues, hero gallantry, sidekick reactions). Putting
all of these in every briefing creates duplication and drift.

### Libraries — Reusable Prose Fragments

Shared behavioral instructions referenced by ID. Multiple descriptors share
them. No variable substitution — same text everywhere it's used.

```yaml
# META-INF/eidos/resources/hanna-barbera-cartoon-style.yaml
id: hanna-barbera-cartoon-style
name: Hanna-Barbera Cartoon Character Style
content: |
  You are a character in a Hanna-Barbera cartoon. You follow these
  conventions:

  EXPOSITORY SOLILOQUY: You frequently narrate your situation aloud...
  EMOTIONAL TELEGRAPHING: State your emotions explicitly and with
  exaggeration...
  SITUATION RECAP: When something significant happens, restate it aloud...
  CATCHPHRASE REPETITION: Use your signature phrases regularly...
```

### Templates — Parameterized Prose

Variable substitution for role-level conventions:

```yaml
# META-INF/eidos/resources/cartoon-villain.yaml
id: cartoon-villain
parameters: [catchphrase, nemesis, scheme_style]
content: |
  As a villain, your catchphrase is "${catchphrase}". Your nemesis is
  ${nemesis}. You monologue your plans in a ${scheme_style} manner.
  You explain your scheme step by step, especially when you believe
  you are about to succeed.
```

### Descriptor References

```yaml
descriptors:
  - agentId: hooded-claw
    resources:
      - ref: hanna-barbera-cartoon-style    # genre layer
      - ref: cartoon-villain                 # role layer
        args:
          catchphrase: "Nyah-ha-ha-HA!"
          nemesis: Penelope Pitstop
          scheme_style: grandiose and theatrical
    briefing: >-                             # character layer
      You are The Hooded Claw...
```

The `SystemPromptRenderer` composes: libraries (in order) → templates
(with substitution) → briefing → disposition axes. Each layer adds
specificity. This is a separate Eidos epic, not part of the Wacky Manor
POC, but the POC is what surfaced the need. For the POC, we inline
everything in the briefing.

### Beyond Wacky Manor

This pattern is useful in professional contexts too:
- A "regulatory communication" library shared across 20 compliance agents
- A "clinical communication" template parameterized per specialty
- A "customer service tone" library referenced by all support agents

## The Plot

### Act 1: Arrival (Entrance Hall)

Everyone arrives. Dastardly welcomes them as tour guide, gives misleading
directions. Characters introduce themselves. Hooded Claw (as Sneekly) is
excessively helpful to Penelope. Ant Hill Mob exchange suspicious glances.
Characters disperse to explore.

### Act 2: Exploration (rooms run in parallel)

**Library — Riddle Puzzle**
A riddle carved into the mantelpiece: "I have cities but no houses, forests
but no trees, water but no fish. What am I?" Characters discuss. Penelope
suggests "a map." Dastardly gives a wrong answer on purpose. Slag Brothers
smash the mantelpiece if asked for help, accidentally revealing a hidden
compartment. Key 1 found inside the rolled-up map.

**Kitchen — Safe Puzzle + Poison Discovery**
Three recipe cards hidden in drawers contain the safe combination digits.
Characters search and share. Dastardly claims his card says "7" when it says
"3." Safe contains Key 2.

When the Hooded Claw enters the Kitchen, he spots rat poison on a high shelf
(only he can see it). Villain monologue on observe channel: "Nyah-ha-HA!
Rat poison! How DELICIOUSLY appropriate!"

**Laboratory — Machine Puzzle + Dynamite Discovery**
A machine requires 3 characters: one holds a lever, one turns a dial, one
reads a gauge. Only Pat Pending can interpret the gauge. Peter Perfect
volunteers to hold the lever to impress Penelope — sleeve gets caught.
Near-disaster. Pat Pending calmly fixes it. Key 3 found in vault.

But first: Rufus needs a wrench from the Kitchen. Sawtooth gnawed through a
wire. Getting the wrench is a fetch quest that forces cross-room movement.

When Hooded Claw enters the Lab, he spots dynamite in a crate (only he can
see it). Plans to rig the Tower staircase.

**Ballroom — Tea Scene (triggered by Hooded Claw having poison)**
Characters gather for tea. Hooded Claw volunteers to pour. Prepares a
poisoned cup for Penelope. Ant Hill Mob are suspicious. Dum-Dum reaches for
sugar and knocks Penelope's cup onto the floor. "Oops! Sorry boss!"

Alternative foil: Blubber Bear wakes up terrified, stumbles into the table,
spills everything.

### Act 3: The Tower

**Staircase Trap**
Hooded Claw has rigged dynamite on the stairs. Muttley trots ahead, sniffs
the fuse, starts snickering. Clyde: "Is dat a FUSE?!" The Mob defuse it
with characteristic bumbling.

Alternative: Sergeant Blast ordered Meekly to guard the staircase. Meekly
found the dynamite and timidly removed it. "I-I didn't want to make a fuss,
sir..."

**Treasure Chest**
Hooded Claw rigged a hidden blade inside. Penelope reaches for the chest.
Ant Hill Mob pull her back ("We saw a spider!"). Peter Perfect opens it
instead — blade mechanism jams because Dastardly stole the spring. (The
spring Big Gruesome was wearing as a bracelet — which Dastardly stole from
Big Gruesome, who stole it from the Lab.)

**Resolution**
Diamond revealed. Dastardly lunges — Muttley trips him. Penelope picks it up.
Hooded Claw slinks away: "FOILED again!" Ant Hill Mob beam. Narrator wraps
up.

## The Hooded Claw's Three Devices

Each attempt follows the cartoon formula: elaborate plan, incompetent
execution, bumbling rescue, Hooded Claw thwarted but nobody realizes he
was trying anything.

| Device | Location | Plan | Foiled by |
|---|---|---|---|
| Rat poison | Kitchen (high shelf) | Spike Penelope's tea at dinner | Ant Hill Mob knock the cup / Blubber Bear crashes into table |
| Dynamite | Lab (crate) | Rig the Tower staircase | Muttley smells the fuse / Meekly removes it |
| Hidden blade | Tower (treasure chest) | Triggers when chest is opened | Ant Hill Mob pull Penelope back / Peter Perfect opens it first / spring is missing |

## Game Mechanics

### Real-Time, Not Turn-Based

Characters act independently and asynchronously. No turns. Each character
agent runs its own loop:

```
observe world state → decide action → act → receive result → observe again
```

CaseHub's architecture is event-driven and async — Qhorus, Engine
choreography, summarisation pipeline. Real-time exercises the actual platform
rather than dumbing it down.

Benefits:
- Hooded Claw sneaks away while others argue — realistic scheming
- Lazy Luke acts at his own pace (barely at all) — character-accurate
- Sergeant Blast acts rapidly — character-accurate
- Overlapping conversations handled by Qhorus commitments
- Narrator keeps up via windowed summarisation

### Movement Model

Door Kickers: Action Squad style. 2D side-view cross-section.

- Characters move left/right through connected rooms on the same floor
- Y changes only at stairs or doors between floors
- Characters are visible as icons at their (x, y) position
- Items visible in rooms as icons

### Visibility Rules

| Object/Event | Who sees it |
|---|---|
| Room contents, furniture, normal objects | Everyone in the room |
| Rat poison, dynamite, blade mechanism | Hooded Claw only |
| Hooded Claw's villain monologues | Audience only (observe channel) |
| Ant Hill Mob's suspicions | Ant Hill Mob + audience |
| Muttley's sniffing alerts | Everyone in the room |
| Dastardly's lies | Everyone hears the lie; narrator flags it for audience |

### Scripted Triggers

Conditions that fire plot beats. The LLMs don't know these exist — they
experience the results in real time.

| Condition | Trigger |
|---|---|
| Hooded Claw enters Kitchen | Poison becomes visible to him |
| Hooded Claw enters Lab | Dynamite becomes visible to him |
| Hooded Claw has poison + Ballroom + Penelope present | Tea scene initiates |
| Ant Hill Mob in Ballroom during tea scene | Cup-knocking foil |
| Dynamite planted + characters approach Tower | Muttley sniff + defuse |
| Penelope reaches for treasure chest | Blade trigger |

### Character-Specific Abilities

| Character | Ability |
|---|---|
| Penelope | Well-read — can interpret riddles and inscriptions |
| Hooded Claw | Discovers hidden dangerous items |
| Ant Hill Mob | Notice suspicious behavior near Penelope |
| Pat Pending | Operate complex machinery |
| Muttley | Smell hidden objects and danger |
| Little Gruesome | Fly into high/tight spaces |
| Sawtooth | Gnaw through wood and rope |
| Slag Brothers | Smash things (sometimes helpful) |

### Static NPC Interaction

Tier 3 characters are scripted interaction trees. When a player character
enters the room and interacts, a predetermined sequence triggers with LLM-
generated dialogue in character voice. The outcome is fixed; the comedy is
in the voice.

Example: Player enters Library, talks to Slag Brothers about riddle.
- Outcome: They smash the mantelpiece, revealing a hidden compartment.
- LLM generates: HOW they do it, what they grunt, the narrator's reaction.

## The Narrator

A separate LLM agent that receives ALL events and produces commentary in the
style of the Wacky Races narrator — breathless, alliterative, dramatic,
omniscient.

"And so our heroes GATHER in the dusty entrance of Doily Manor, UTTERLY
UNAWARE that DANGER lurks behind every cobweb! The Hooded Claw adjusts his
disguise and flashes a smile SO sinister it could curdle MILK!"

### Summarisation Pipeline Mapping

| Level | What | Narrator output |
|---|---|---|
| L1 (raw events) | Each character action and dialogue line | Real-time commentary snippets |
| L2 (episodes) | Per-room activity summaries (keyed by room) | Room scene descriptions |
| L3 (phases) | Per-act summaries | Act transitions, dramatic recaps |
| L4 (narrative) | Overall story summary | Epilogue, "tune in next time!" |

## Human-in-the-Loop — Interactive Play

Via Claudony and Qhorus, the audience participates — not just watches.

- Audience members connect via web UI (or Slack/Discord via Connectors)
- They can send messages to any character
- Characters respond in character
- The audience becomes part of the show

Examples:
- "Penelope, don't trust Sneekly!" → "Why, what a peculiar thing to say!
  Mr. Sneekly has been nothin' but a perfect gentleman!"
- "Dastardly, where's the treasure?" → "The treasure is most certainly in
  the... CELLAR! Yes! Definitely not the Tower. Mehehehe."
- "Muttley, what do you smell?" → "*sniff sniff* Rassafrassa... *points
  nose toward kitchen* ...hehehehe!"

## Character Memory (Neocortex)

Characters remember previous rooms and interactions.

- Episodic: "Dastardly said the combination was 7-3-9 but it was wrong"
- Semantic: "Sneekly has been suspiciously helpful" (accumulated by Ant Hill Mob)
- Working: Current room state, items in inventory

After Dastardly lies once, characters who remember distrust his future
directions. Ant Hill Mob's suspicion of Sneekly grows across rooms.

## CaseHub Capabilities Exercised

17 existing capabilities, 3-4 new requirements driven:

**Existing:** Eidos descriptors, SystemPromptRenderer, behavioral signals,
CapabilityHealth, Qhorus work/observe channels, speech acts, commitments,
presence, topics, spaces, WebSocket observer, Engine choreography, Blocks
summarisation, KeyedAccumulator, channel adapters, Neocortex agent memory/
RAG/CBR, Ledger audit trail, trust scoring, Connectors, Work HIL.

**New requirements driven:**
- Eidos goals/aims/constraints fields on AgentDescriptor
- Engine HTN planning strategy (compound PlanItems, unified execution model)
- 2D mansion viewer component (Pages or standalone)
- Room-scoped channel visibility in Qhorus

## Parked Ideas (for later iterations)

### Coding Puzzles
Characters cooperatively write software — each writes a component, they must
integrate. Interface negotiation IS the interaction. Personality affects code
style. Dastardly writes obfuscated code with subtle bugs. Pitstop writes
clean, documented code. Tests pass or fail objectively. Parked for now but
a strong candidate for a second scenario targeting developer audiences.

### Elimination + Recombination
After each room, weakest contributor voted out. Creates dual incentive:
cooperate (solve puzzle) AND compete (don't get eliminated). Eliminated
characters return in new team configurations. Parked — adds complexity
without validating the core premise first.

### Race Variant
The logistics example from the summarisation spec, but with Wacky Races
characters. L1 race events → L2 incidents → L3 phases → L4 commentary.
Coaching agents adapt advice to personality. Parked — mansion is a richer
interaction setting.

### Multi-Scenario Platform
Multiple scenarios with the same cast — mansion, race, coding challenge,
mystery dinner. Scenario definitions as YAML. Replay and compare different
LLM backends on the same scenario.

## References

- Wacky Races (Hanna-Barbera, 1968-69) — character voices and dynamics
- The Perils of Penelope Pitstop (1969-70) — Hooded Claw, Ant Hill Mob
- Door Kickers: Action Squad — 2D ant-farm cross-section visual style
- Maniac Mansion / Sam & Max Hit the Road — point-and-click adventure structure
- Stanford Generative Agents (Smallville) — memory/reflection/planning architecture
- Paracosm — deterministic kernel, personality-driven agents
- CaseHub unified execution model — HTN, compound PlanItems, choreography
- CaseHub blocks summarisation spec — L1-L4 pipeline, KeyedAccumulator
