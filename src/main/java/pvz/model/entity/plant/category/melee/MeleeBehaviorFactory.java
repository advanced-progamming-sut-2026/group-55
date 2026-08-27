package pvz.model.entity.plant.category.melee;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PassivePlantBehavior;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class MeleeBehaviorFactory {

    private MeleeBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        MeleeProfile profile = MeleeProfiles.from(spec);
        if (profile == null) {
            return new PassivePlantBehavior(owner);
        }

        return switch (profile.kind()) {
            case BONK_CHOY -> new DirectionalMeleeBehavior(
                    owner,
                    profile,
                    false
            );
            case PHAT_BEET -> new AreaMeleeBehavior(owner, profile);
            case CHOMPER -> new ChomperBehavior(owner, profile);
            case WASABI_WHIP -> new DirectionalMeleeBehavior(
                    owner,
                    profile,
                    true
            );
            case KIWIBEAST -> new KiwibeastBehavior(owner, profile);
        };
    }

    public static boolean isSupported(PlantSpec spec) {
        return MeleeProfiles.isSupported(spec);
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return MeleeProfiles.supportsPlantFood(spec);
    }
}
