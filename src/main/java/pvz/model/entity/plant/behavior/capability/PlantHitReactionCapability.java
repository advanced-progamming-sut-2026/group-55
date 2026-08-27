package pvz.model.entity.plant.behavior.capability;

import pvz.model.entity.plant.hit.PlantHitContext;

public interface PlantHitReactionCapability {

    void onPlantHit(PlantHitContext context);
}
