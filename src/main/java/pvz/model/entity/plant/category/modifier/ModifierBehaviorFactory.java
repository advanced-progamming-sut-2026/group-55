package pvz.model.entity.plant.category.modifier;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PassivePlantBehavior;
import pvz.model.entity.plant.category.wall.WallBehaviorFactory;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class ModifierBehaviorFactory {

    private ModifierBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        ModifierKind kind = ModifierProfiles.kindOf(spec);

        if (kind == null) {
            return new PassivePlantBehavior(owner);
        }

        return switch (kind) {
            case HYPNO_SHROOM -> new HypnoShroomBehavior(owner);
            case LILY_PAD -> new LilyPadBehavior(owner);
            case TORCHWOOD -> new TorchwoodBehavior(owner);
            case IMITATER ->
                    WallBehaviorFactory.createBasicWallLike(
                            owner,
                            spec
                    );
        };
    }

    public static boolean isSupported(PlantSpec spec) {
        return ModifierProfiles.isSupported(spec);
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return ModifierProfiles.supportsPlantFood(spec);
    }
}
