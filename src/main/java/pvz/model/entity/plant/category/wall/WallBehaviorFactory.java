package pvz.model.entity.plant.category.wall;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class WallBehaviorFactory {
    private WallBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        boolean blocksVaulting = WallProfiles.blocksVaulting(spec);

        if (spec.getTags().contains(PlantTag.EXPLOSIVE)) {
            return new ExplodeONutBehavior(
                    owner,
                    blocksVaulting,
                    spec.getBaseHp(),
                    parseExplosionDamage(spec)
            );
        }

        if (supportsPlantFood(spec)) {
            return new ArmoredWallBehavior(
                    owner,
                    blocksVaulting,
                    spec.getBaseHp()
            );
        }

        return new WallBehavior(
                owner,
                blocksVaulting
        );
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return WallProfiles.supportsArmorPlantFood(spec);
    }

    private static double parseExplosionDamage(PlantSpec spec) {
        try {
            double damage = Double.parseDouble(
                    spec.getDamage().strip()
            );

            if (damage <= 0) {
                throw new IllegalArgumentException(
                        "explosion damage must be positive"
                );
            }

            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid explosion damage for "
                            + spec.getName()
                            + ": "
                            + spec.getDamage(),
                    exception
            );
        }
    }
}
