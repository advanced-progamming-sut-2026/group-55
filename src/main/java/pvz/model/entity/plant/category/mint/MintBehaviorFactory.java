package pvz.model.entity.plant.category.mint;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class MintBehaviorFactory {

    private MintBehaviorFactory() {
    }

    public static boolean isMint(PlantSpec spec) {
        return spec != null && spec.getTags().contains(PlantTag.MINT);
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        if (!isMint(spec)) {
            throw new IllegalArgumentException(
                    spec.getName() + " is not a mint"
            );
        }

        return new MintBehavior(owner, spec);
    }
}
