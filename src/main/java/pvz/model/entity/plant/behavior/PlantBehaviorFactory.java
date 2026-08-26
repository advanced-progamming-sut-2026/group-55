package pvz.model.entity.plant.behavior;

import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.category.explosive.ExplosiveBehaviorFactory;
import pvz.model.entity.plant.category.lobber.LobberBehaviorFactory;
import pvz.model.entity.plant.category.melee.MeleeBehaviorFactory;
import pvz.model.entity.plant.category.shooter.ShooterBehaviorFactory;
import pvz.model.entity.plant.category.strikethrough.StrikeThroughBehaviorFactory;
import pvz.model.entity.plant.category.sun.SunProducerBehavior;
import pvz.model.entity.plant.category.wall.WallBehaviorFactory;

public final class PlantBehaviorFactory {

    private PlantBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");

        Objects.requireNonNull(spec, "plant spec cannot be null");

        return switch (spec.getCategory()) {

            case SHOOTER ->
                    ShooterBehaviorFactory.create(
                            owner,
                            spec
                    );

            case SUN_PRODUCER -> new SunProducerBehavior(owner, spec);

            case STRIKE_THROUGH ->
                    StrikeThroughBehaviorFactory.create(
                            owner,
                            spec
                    );

            case WALL ->
                    WallBehaviorFactory.create(
                            owner,
                            spec
                    );

            case EXPLOSIVE ->
                    ExplosiveBehaviorFactory.create(
                            owner,
                            spec
                    );

            case LOBBER ->
                    LobberBehaviorFactory.create(
                            owner,
                            spec
                    );

            case MELEE ->
                    MeleeBehaviorFactory.create(
                            owner,
                            spec
                    );

            default -> new PassivePlantBehavior(owner);
        };
    }
}
