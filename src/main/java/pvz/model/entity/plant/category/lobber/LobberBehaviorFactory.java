package pvz.model.entity.plant.category.lobber;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PassivePlantBehavior;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class LobberBehaviorFactory {
    private LobberBehaviorFactory() {
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return spec != null && LobberProfiles.supports(spec);
    }

    public static PlantBehavior create(
            Plant owner,
            PlantSpec spec
    ) {
        Objects.requireNonNull(
                owner,
                "owner plant cannot be null"
        );
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (!LobberProfiles.supports(spec)) {
            return new PassivePlantBehavior(owner);
        }

        return new LobberBehavior(
                owner,
                spec,
                () -> ThreadLocalRandom.current().nextDouble()
        );
    }
}
