package pvz.model.entity.plant.category.homing;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PassivePlantBehavior;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class HomingBehaviorFactory {

    private HomingBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        HomingProfile profile = HomingProfiles.from(spec);

        if (profile == null) {
            return new PassivePlantBehavior(owner);
        }

        return switch (profile.kind()) {
            case CAULIPOWER -> new CaulipowerBehavior(owner, profile);

            case ELECTRIC_BLUEBERRY ->
                    new ElectricBlueberryBehavior(owner, profile);

            case MAGNET_SHROOM ->
                    new MagnetShroomBehavior(owner, profile);

            case CAT_TAIL -> new CatTailBehavior(owner, profile);
        };
    }

    public static boolean isSupported(PlantSpec spec) {
        return HomingProfiles.isSupported(spec);
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return HomingProfiles.supportsPlantFood(spec);
    }
}
