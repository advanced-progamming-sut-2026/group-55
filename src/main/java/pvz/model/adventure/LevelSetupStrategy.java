package pvz.model.adventure;

import pvz.model.selection.PlantSelectionRules;

/**
 * Defines the setup route for one family of Adventure levels.
 *
 * <p>Special-level implementations can later replace the unsupported route
 * without adding level-ID conditions to the Level Selection screen.</p>
 */
@FunctionalInterface
public interface LevelSetupStrategy {
    LevelEntryRoute entryRoute(LevelSpec level);

    /**
     * Rules supplied when this strategy enters ordinary plant selection.
     * Normal levels use the Phase 1 default of eight unrestricted slots.
     */
    default PlantSelectionRules plantSelectionRules(LevelSpec level) {
        return PlantSelectionRules.normal();
    }

    /**
     * Player-facing mission text shown before simulation time begins.
     * Special-level strategies can replace this when their objectives exist.
     */
    default LevelObjectivePresentation objectivePresentation(LevelSpec level) {
        return LevelObjectivePresentation.forObjective(level.objectiveType());
    }
}
