package pvz.model.entity.plant.behavior.capability;

/**
 * Exposes a plant's logical growth stage to presentation code without
 * coupling the view to a concrete behavior/profile implementation.
 */
public interface GrowthStageCapability {

    int getGrowthStage(long currentTick);
}
