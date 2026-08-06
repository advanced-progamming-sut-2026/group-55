package pvz.model.entity.plant.strikethrough;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;

public final class StrikeThroughProfiles {
    private static final int FULL_BOARD_RANGE =
            Integer.MAX_VALUE;

    private static final int FUME_SHROOM_RANGE_TILES = 4;

    private StrikeThroughProfiles() {
    }

    public static StrikeThroughProfile from(
            PlantSpec spec
    ) {
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (spec.getCategory()
                != PlantCategory.STRIKE_THROUGH) {
            throw new IllegalArgumentException(
                    spec.getName()
                            + " is not a strike-through plant"
            );
        }

        int rangeTiles = switch (spec.getName()
                .strip()
                .toLowerCase(Locale.ROOT)) {

            case "cactus" -> FULL_BOARD_RANGE;
            case "fume-shroom" -> FUME_SHROOM_RANGE_TILES;

            default -> throw new IllegalArgumentException(
                    "unsupported strike-through plant: "
                            + spec.getName()
            );
        };

        return new StrikeThroughProfile(
                parseDamage(spec),
                0,
                List.of(
                        new ShotPath(
                                0,
                                ShotVector.RIGHT,
                                1
                        )
                ),
                ProjectileType.NORMAL,
                rangeTiles,
                ProjectileHitLimit.unlimited()
        );
    }

    private static double parseDamage(
            PlantSpec spec
    ) {
        try {
            double damage = Double.parseDouble(
                    spec.getDamage()
            );

            if (!Double.isFinite(damage)
                    || damage < 0) {
                throw new IllegalArgumentException(
                        "strike-through damage must be finite and non-negative: "
                                + spec.getDamage()
                );
            }

            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid strike-through damage: "
                            + spec.getDamage(),
                    exception
            );
        }
    }
}
