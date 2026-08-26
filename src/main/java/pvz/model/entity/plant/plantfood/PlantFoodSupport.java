package pvz.model.entity.plant.plantfood;

import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.category.explosive.ExplosiveBehaviorFactory;
import pvz.model.entity.plant.category.lobber.LobberBehaviorFactory;
import pvz.model.entity.plant.category.shooter.bowlingbulb.BowlingBulbPlantFoodProfile;
import pvz.model.entity.plant.category.shooter.plantfood.ShooterPlantFoodProfiles;
import pvz.model.entity.plant.category.sun.SunProfiles;
import pvz.model.entity.plant.category.wall.WallBehaviorFactory;

public final class PlantFoodSupport {

    private PlantFoodSupport() {
    }

    public static boolean isImplemented(PlantSpec spec) {
        if (spec == null) {
            return false;
        }

        return switch (spec.getCategory()) {

            case SHOOTER ->
                    BowlingBulbPlantFoodProfile.supports(spec)
                            || ShooterPlantFoodProfiles.from(spec) != null;

            case SUN_PRODUCER ->
                    SunProfiles.supportsPlantFood(spec);

            case LOBBER ->
                    LobberBehaviorFactory.supportsPlantFood(spec);

            case EXPLOSIVE ->
                    ExplosiveBehaviorFactory.supportsPlantFood(spec);

            case WALL ->
                    WallBehaviorFactory.supportsPlantFood(spec);

            default -> false;
        };
    }
}
