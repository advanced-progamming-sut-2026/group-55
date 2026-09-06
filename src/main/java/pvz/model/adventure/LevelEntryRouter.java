package pvz.model.adventure;

import java.util.Map;
import java.util.Objects;
import pvz.model.selection.PlantSelectionRules;

/** Resolves the next UI/setup flow for a selected Adventure level. */
public final class LevelEntryRouter {
    private final Map<LevelType, LevelSetupStrategy> strategies;

    public LevelEntryRouter() {
        this(Map.of(
                LevelType.NORMAL,
                level -> LevelEntryRoute.PLANT_SELECTION
        ));
    }

    LevelEntryRouter(Map<LevelType, LevelSetupStrategy> strategies) {
        this.strategies = Map.copyOf(Objects.requireNonNull(
                strategies,
                "level setup strategies cannot be null"
        ));
    }

    public PlantSelectionRules plantSelectionRules(LevelSpec level) {
        Objects.requireNonNull(level, "level cannot be null");
        LevelSetupStrategy strategy = strategies.get(level.type());
        if (strategy == null
                || strategy.entryRoute(level) != LevelEntryRoute.PLANT_SELECTION) {
            throw new IllegalStateException(
                    "level does not use ordinary plant selection: " + level.id()
            );
        }
        return Objects.requireNonNull(
                strategy.plantSelectionRules(level),
                "level setup strategy returned null plant-selection rules"
        );
    }

    public LevelObjectivePresentation objectivePresentation(LevelSpec level) {
        Objects.requireNonNull(level, "level cannot be null");
        LevelSetupStrategy strategy = strategies.get(level.type());
        if (strategy == null) {
            throw new IllegalStateException(
                    "no setup strategy for level: " + level.id()
            );
        }
        return Objects.requireNonNull(
                strategy.objectivePresentation(level),
                "level setup strategy returned a null objective presentation"
        );
    }

    public LevelEntryRoute route(LevelSpec level) {
        Objects.requireNonNull(level, "level cannot be null");
        LevelSetupStrategy strategy = strategies.get(level.type());
        if (strategy == null) {
            return LevelEntryRoute.UNSUPPORTED_SETUP;
        }
        return Objects.requireNonNull(
                strategy.entryRoute(level),
                "level setup strategy returned a null route"
        );
    }
}
