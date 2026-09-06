package pvz.model.minigame;

import java.util.Objects;

/**
 * Stable navigation payload for one minigame stage.
 * Phase 7 can route this value to the concrete gameplay screen without
 * re-parsing display text or rebuilding ids in the UI layer.
 */
public record MinigameStageRoute(String minigameId, int stageNumber) {
    public MinigameStageRoute {
        minigameId = MinigameSpec.normalizeId(minigameId);
        if (stageNumber <= 0) {
            throw new IllegalArgumentException(
                    "minigame stage number must be positive"
            );
        }
    }

    public static MinigameStageRoute of(
            MinigameSpec spec,
            int stageNumber
    ) {
        Objects.requireNonNull(spec, "minigame spec cannot be null");
        if (!spec.hasStage(stageNumber)) {
            throw new IllegalArgumentException(
                    "invalid stage " + stageNumber + " for " + spec.id()
            );
        }
        return new MinigameStageRoute(spec.id(), stageNumber);
    }

    public String stageId(MinigameCatalog catalog) {
        Objects.requireNonNull(catalog, "minigame catalog cannot be null");
        return catalog.require(minigameId).stageId(stageNumber);
    }
}
