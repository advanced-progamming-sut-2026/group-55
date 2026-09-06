# A10 - Level Selection integration notes

## Scope closed in A10

A10 owns the Adventure navigation path up to entering the next setup screen:

```text
Game Menu -> Chapter -> Level Selection -> level entry route
```

The current runtime intentionally supports only normal Adventure levels. The
experimental Ancient Egypt data can therefore be used to validate the complete
A10 path without pretending that the rest of Adventure is finished.

The Phase 1/2 contracts covered here are:

- Chapter name, progress and locked/unlocked state are visible.
- Selecting a Chapter shows its configured levels.
- A locked level cannot be entered.
- Completing a level makes the next sequential level available.
- Completing all configured levels of a Chapter makes the next non-empty
  Chapter available.
- Completed levels remain replayable and do not increment `clearedStages`
  again.
- A newly available Adventure level creates a News item.
- Level selection is data-driven by `LevelCatalog`; no level ID is hardcoded in
  Java.

## Progress fixes added in A10

`LevelProgressService` is now the single place that answers both level access
and Chapter menu access.

Important cases now handled:

1. **Out-of-order completion**
   A level may be made available by a future Quest reward. If a later level is
   completed first and the missing earlier level is completed afterwards, the
   Chapter completion check still runs and the next Chapter can unlock.

2. **Accurate unlock result**
   `CompletionResult.unlockedLevelId` is only populated when that level became
   newly available. A level already available through a reward is not reported
   as newly unlocked again.

3. **Catalog/save reconciliation**
   `reconcileProgress(User)` persists Chapter access that is already implied by
   completed previous-Chapter progress and the current catalog. This is meant
   for development and migration cases such as adding Frostbite levels after a
   user had already completed all currently configured Ancient Egypt levels.
   Reconciliation only adds access; it never relocks content.

4. **Direct level reward remains reachable**
   `isChapterAccessible` allows the Adventure menu to open a Chapter containing
   an explicitly reward-unlocked/completed level even if sequential Chapter
   progression has not opened that Chapter. This does not automatically make
   the Chapter's other levels sequentially available.

5. **Unlock News**
   A first-clear that makes a new Adventure level available adds exactly one
   `Level Unlocked` News item. Replay does not duplicate it.

## GUI cleanup in A10

`LevelSelectionScreen` now uses the shared menu typography, hover behavior and
currency badge used by the newer screens. This keeps the screen consistent
with the Phase 2 GUI without introducing new art requirements.

Locked levels remain disabled. Available and completed levels stay selectable
(the latter for replay).

## Infrastructure intentionally prepared, not completed

### Special Adventure levels

Phase 1 requires each Chapter to eventually contain four stages: one normal
stage, two special stages, and a final boss stage (the boss gameplay belongs to
Phase 2 work).

A10 adds `LevelSetupStrategy` plus `LevelEntryRouter` / `LevelEntryRoute`
between Level Selection and the next screen. Today:

```text
NORMAL  -> PLANT_SELECTION
SPECIAL -> UNSUPPORTED_SETUP
```

This is deliberate. When special levels are implemented, register their real strategy and special
setup metadata at this boundary rather than putting
`if`/`switch` statements on level IDs inside `LevelSelectionScreen`.

Examples that must be completed later:

- Conveyor Belt: skip ordinary plant selection and enter its own setup.
- Locked Plants: enter a constrained/pre-filled plant-selection setup. A11 now
  provides the `PlantSelectionRules` contract for that future strategy.
- Save Our Seeds / Timed War / Night Ops / Dead Line / Love Your Plants /
  Plant What You Get: attach their own objective/setup rules.
- Boss stage: use boss-specific setup and the Phase 2 boss UI/runtime.
- Chapter terrain/rules: Ancient Egypt tombs/tornado, Frostbite ice rules,
  Big Wave Beach water, Dark Ages night/necromancy.

The current CSV loader should continue rejecting runtime data that is not yet
supported. Do not make `SPECIAL` data executable as a normal level merely to
show it in the menu.

### Full Adventure data

The current two Ancient Egypt levels are test data, not the final Adventure.
When Adventure is completed, populate all four Chapters and their four stages
through the data files. Adding ordinary levels must remain data-only and must
not require Java conditions on level IDs.

### Quest level-unlock integration

`AdventureProgress` already has reward-unlocked level IDs. Later, when real
quests begin granting Adventure levels, route that reward through an Adventure
progress facade/service so that access, persistence and News are emitted from
one place. Do not let `QuestRewardService` and Adventure screens implement
separate unlock rules.

### Minigames

Minigame progression remains separate from Adventure progression. A10 should
not move Vase Breaker, Wall-nut Bowling or I, Zombie into `LevelCatalog`.
When their actual stages are implemented, keep `MinigameProgressService` as the
owner of their stage progression and add the equivalent unlock notification at
that boundary.

### Phase 3

No network behavior belongs in A10. Phase 3 keeps ordinary Adventure stages
single-player and makes I, Zombie the required network multiplayer mode.
The useful A10 preparation is architectural: the screen asks
`LevelProgressService` for access instead of deriving progression itself, so
later the persistence source can move from local save data to server-backed
account data without putting networking logic in `LevelSelectionScreen`.

## Verification added

Model-level regression coverage was added for:

- sequential unlock and replay behavior;
- out-of-order clear followed by Chapter unlock;
- not reporting a reward-unlocked level as newly unlocked;
- restoring Chapter access from an older save/current catalog combination;
- direct reward access to a locked Chapter;
- exactly-once level-unlock News;
- normal vs unsupported-special level entry routing.
