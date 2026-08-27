package pvz.model.entity.plant.category.strikethrough;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PassivePlantBehavior;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class StrikeThroughBehaviorFactory {

    private StrikeThroughBehaviorFactory() {
    }

    public static PlantBehavior create(
            Plant owner,
            PlantSpec spec
    ) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        if (!StrikeThroughProfiles.isSupported(spec)) {
            return new PassivePlantBehavior(owner);
        }

        return new StrikeThroughBehavior(owner, spec);
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return StrikeThroughProfiles.supportsPlantFood(spec);
    }
}
