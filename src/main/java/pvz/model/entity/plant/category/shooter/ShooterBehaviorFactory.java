package pvz.model.entity.plant.category.shooter;

import java.util.Locale;
import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.behavior.PlantBehavior;
import pvz.model.entity.plant.category.shooter.bowlingbulb.BowlingBulbBehavior;

public final class ShooterBehaviorFactory {

    private ShooterBehaviorFactory() {
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

        String plantName = spec.getName()
                .strip()
                .toLowerCase(Locale.ROOT);

        if (plantName.equals("bowling bulb")) {
            return new BowlingBulbBehavior(owner, spec);
        }

        return new ShooterBehavior(owner, spec);
    }
}
