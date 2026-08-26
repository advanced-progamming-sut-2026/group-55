package pvz.model.entity.plant.behavior.capability;

import pvz.model.entity.plant.placement.PlantPlacementTarget;

public interface TargetTilePlacementCapability {

    boolean canTarget(PlantPlacementTarget target);
}
