package pvz.model.entity.plant.wall;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class WallBehaviorFactory {
    private static final int TALL_NUT_ID = 45;
    private static final int EXPLODE_O_NUT_ID = 49;

    private WallBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        if (spec.getId() == EXPLODE_O_NUT_ID) {
            return new ExplodeONutBehavior(
                    owner,
                    parseExplosionDamage(spec)
            );
        }

        return new WallBehavior(
                owner,
                spec.getId() == TALL_NUT_ID
        );
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
