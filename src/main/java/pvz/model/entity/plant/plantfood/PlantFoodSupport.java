package pvz.model.entity.plant.plantfood;

import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.category.explosive.ExplosiveBehaviorFactory;
import pvz.model.entity.plant.category.homing.HomingBehaviorFactory;
import pvz.model.entity.plant.category.lobber.LobberBehaviorFactory;
import pvz.model.entity.plant.category.melee.MeleeBehaviorFactory;
import pvz.model.entity.plant.category.mint.MintBehaviorFactory;
import pvz.model.entity.plant.category.modifier.ModifierBehaviorFactory;
import pvz.model.entity.plant.category.shooter.bowlingbulb.BowlingBulbPlantFoodProfile;
import pvz.model.entity.plant.category.strikethrough.StrikeThroughBehaviorFactory;
import pvz.model.entity.plant.category.shooter.plantfood.ShooterPlantFoodProfiles;
import pvz.model.entity.plant.category.sun.SunProfiles;
import pvz.model.entity.plant.category.wall.WallBehaviorFactory;

public final class PlantFoodSupport {

    private PlantFoodSupport() {
    }

    public static boolean isImplemented(PlantSpec spec) {
        if (spec == null || MintBehaviorFactory.isMint(spec)) {
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

            case STRIKE_THROUGH ->
                    StrikeThroughBehaviorFactory.supportsPlantFood(spec);

            case EXPLOSIVE ->
                    ExplosiveBehaviorFactory.supportsPlantFood(spec);

            case HOMING ->
                    HomingBehaviorFactory.supportsPlantFood(spec);

            case MELEE ->
                    MeleeBehaviorFactory.supportsPlantFood(spec);

            case MODIFIER ->
                    ModifierBehaviorFactory.supportsPlantFood(spec);

            case WALL ->
                    WallBehaviorFactory.supportsPlantFood(spec);

            default -> false;
        };
    }
}
