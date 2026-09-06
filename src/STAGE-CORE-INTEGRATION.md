# Stage Core integration notes

This checkpoint closes the shared runtime/HUD work for the two current normal
Ancient Egypt levels. The following items are intentionally deferred until the
corresponding Adventure features exist; the current changes only provide the
boundaries they need.

## Ancient Egypt chapter mechanics

- Initial tombstone placement still needs level/setup data rather than hardcoded
  coordinates in `BattleScreen`.
- Final-wave tornado entry still needs an Ancient Egypt setup/mechanic component.
  It should alter spawn entry positions without special-casing level IDs in the
  generic wave HUD.

## Full Adventure

- Boss integration must make Lawn Mower kill policy respect boss immunity.
- The Egypt background currently remains appropriate for the two implemented
  levels. Before Frostbite/Beach/Dark Ages become executable, battle visuals
  should be selected by a chapter visual/theme strategy instead of a screen
  switch on chapter IDs.
- `LevelObjectivePresentation` is now routed through `LevelSetupStrategy`.
  Special objectives should override that presentation when their actual
  runtime rules are implemented.
- `WaveProgressSnapshot` is generic and should be reused by special levels that
  still use waves. Objective-specific progress (timers, protected plants,
  deadlines, etc.) should be a separate read model rather than being forced into
  wave progress.

## Minigames

Vasebreaker, Wall-nut Bowling and I, Zombie should keep their own setup/runtime
flows. Reuse the compact battle HUD actors only where their mechanics actually
match; do not route them through ordinary Adventure plant selection merely to
reuse UI.
