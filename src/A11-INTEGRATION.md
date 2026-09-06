# A11 - Plant Selection integration notes

## Scope closed in A11

A11 owns the normal Adventure loadout flow:

```text
Level Selection -> Plant Selection -> GameSession
```

The current playable Adventure data still contains only ordinary test levels.
A11 therefore closes the Phase 1/2 contracts that can be validated without
pretending that special Adventure stages or minigame gameplay already exist.

Implemented/verified in this slice:

- owned/locked plants are distinguished and only owned, level-allowed plants
  can be selected;
- duplicate selection is rejected;
- ordinary stages default to eight slots;
- selected plants show image, effective sun cost and boost state;
- plant level, seed progress, family and effective sun cost are visible in the
  main plant grid;
- collection-like filters are available for family, ownership and upgrade
  readiness;
- upgrade remains available from plant selection;
- manual 2-diamond boost is stage-wide, but is now transactional: it is queued
  in Plant Selection and charged only when the loadout is committed by
  starting the level;
- backing out or removing a manually boosted plant no longer loses diamonds;
- a greenhouse stored boost does not force that plant into every loadout;
- a greenhouse stored boost is not consumed at level start; on the first
  successful placement it is consumed and activates the ordinary stage-wide
  boost for that plant for the remainder of the current run;
- failed placement attempts do not consume greenhouse boosts;
- battle seed packets visibly retain a BOOST marker while a manual boost, a
  pending greenhouse boost, or a greenhouse boost activated in this run is
  active;
- selected loadout cards and in-battle seed packets share
  `PlantCompactCardContent`, so image/cost/status presentation is not duplicated;
- the shared BaseScreen currency/debug badge is used instead of hidden
  click-to-cheat currency behavior.

## Boost model after A11

Two boost sources are intentionally separate.

### Manual boost

Phase 1 says that spending two diamonds on a plant makes every placement of
that plant in the current stage trigger its Plant Food effect immediately.
Accordingly:

```text
Plant Selection: queue manual boost
Start Level: charge 2 diamonds per queued plant
GameSession: manual boost remains active for the whole stage
```

The charge is deferred until Start so cancelling the loadout cannot strand a
persistent currency charge for transient screen state.

### Greenhouse stored boost

The Greenhouse contract says that the stored boost is used on the first use of
that plant in a level. It now follows:

```text
User.storedBoosts
        |
        | selected plant only
        v
GameSession.pendingStoredBoosts
        |
        | first successful placement
        v
StoredPlantBoostAccess.consume(...)
        |
        +--> persistent User boost removed and saved
        +--> stage-wide boost becomes active for the current run
        +--> this and later placements receive Plant Food automatically
```

`StoredPlantBoostAccess` is deliberately a persistence boundary rather than a
reference from battle logic to `UserManager`. A future server-backed account
implementation can consume the same reward without putting networking code in
`GameSession` or `GameController`.

Restarting a battle does not recreate a consumed stored boost: the session
checks the persistent access boundary in addition to its config snapshot. A
boost that had already been consumed belongs to the previous run and is not
regranted by Restart.

## Plant-selection rules infrastructure

`PlantSelectionRules` is now the data-only contract for stage-specific loadout
restrictions. The normal strategy returns:

```text
maxSlots      = 8
lockedSlots   = 0
forcedPlants  = []
allowedPlants = unrestricted
bannedPlants  = []
bannedFamilies = []
```

The controller already understands:

- total/max slots;
- locked empty slots;
- forced/non-removable plants;
- explicit allowed plants;
- banned plants;
- banned families.

`LevelSetupStrategy` exposes `plantSelectionRules(LevelSpec)` and
`LevelEntryRouter` passes those rules to `PlantSelectionScreen`. This is the
boundary to extend later; do not hardcode level IDs in the screen/controller.

## Intentionally deferred until full Adventure

The Phase 1 special-stage rules are not invented in A11 because the current
CSV schema/loader has no authoritative per-level loadout metadata for them.
When those stages are implemented, populate real data and return the proper
`PlantSelectionRules` from their setup strategy.

Pending examples:

- **Locked Plants**: configure locked slots, forced plants and family/plant
  restrictions using `PlantSelectionRules`.
- **Conveyor Belt**: continue to skip ordinary Plant Selection entirely via the
  A10 `LevelEntryRouter`.
- **Plant What You Get**: provide the stage-specific unavailable producer list
  (and its other setup rules) from data/strategy rather than adding conditions
  to A11.
- **Save Our Seeds / Timed War / Night Ops / Dead Line / Love Your Plants**:
  keep their objective/runtime rules outside Plant Selection unless the final
  stage data actually imposes a loadout restriction.
- **Boss stages**: use their own setup route; do not force them through normal
  plant selection simply to reuse this screen.

The current `AdventureCsvLoader` should continue rejecting unsupported stage
kinds until their runtime/setup strategy is real.

## Intentionally deferred until minigame implementation

Vase Breaker and Wall-nut Bowling do not use ordinary plant selection. I,
Zombie selects zombies instead of plants. Their future loadout/setup UIs may
reuse compact plant/seed visual components where appropriate, but they must not
be forced into `PlantSelectionController`.

Minigame progression also remains separate from Adventure progression.

## Phase 3 boundary

No Phase 3 network gameplay is implemented in A11. Ordinary Adventure remains
single-player. The useful preparation is `StoredPlantBoostAccess`: account
persistence for a consumed greenhouse reward can later become server-backed
without changing the battle/session rules.

I, Zombie networking belongs to its minigame implementation, not to ordinary
Adventure plant selection.

## Verification added

Regression coverage now includes:

- stored boosts outside the selected loadout do not block starting a level;
- selected greenhouse boosts remain stored after Start and are consumed on the
  first successful placement;
- manual boosts do not charge diamonds while the loadout is still cancellable;
- normal selection rules expose the Phase 1 eight-slot default;
- invalid forced/locked-slot rule combinations are rejected.
