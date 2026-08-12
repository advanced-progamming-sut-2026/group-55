package pvz.model.entity.plant.plantfood;

import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.shooter.plantfood.ShooterPlantFoodProfiles;
import pvz.model.entity.plant.sun.SunProfiles;

public final class PlantFoodSupport {

    private PlantFoodSupport() {
    }

    public static boolean isImplemented(PlantSpec spec) {
        if (spec == null) {
            return false;
        }

        return switch (spec.getCategory()) {

            case SHOOTER ->
                    ShooterPlantFoodProfiles.from(spec) != null;

            case SUN_PRODUCER ->
                    SunProfiles.hasProfileFor(spec);

            default -> false;
        };
    }
}
