package pvz.model.entity.plant.behavior;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.shooter.ShooterBehavior;
import pvz.model.entity.plant.sun.SunProducerBehavior;

public final class PlantBehaviorFactory {

    private PlantBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");

        Objects.requireNonNull(spec, "plant spec cannot be null");

        return switch (spec.getCategory()) {

            case SHOOTER -> new ShooterBehavior(owner, spec);

            case SUN_PRODUCER -> new SunProducerBehavior(owner, spec);

            default -> new PassivePlantBehavior(owner);
        };
    }
}
