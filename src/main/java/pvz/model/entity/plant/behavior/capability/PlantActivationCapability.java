package pvz.model.entity.plant.behavior.capability;

import pvz.model.entity.plant.lifecycle.PlantThreat;

/**
 * Describes a committed plant activation that is still resolving.
 *
 * <p>While this phase is active the plant may remain targetable so hostile
 * behaviors can still run, while selected threats are rejected by the plant
 * itself. This keeps targeting/animation semantics separate from whether an
 * incoming interaction is allowed to change gameplay state.</p>
 */
public interface PlantActivationCapability {

    boolean isActivationActive();

    default boolean blocksPlantFoodDuringActivation() {
        return true;
    }

    default boolean blocksThreatDuringActivation(PlantThreat threat) {
        return switch (threat) {
            case TRANSIENT_EFFECT_COMPLETION, SYSTEM_CLEANUP -> false;
            default -> true;
        };
    }
}
