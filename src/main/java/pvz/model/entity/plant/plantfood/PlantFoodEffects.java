package pvz.model.entity.plant.plantfood;

import pvz.model.entity.plant.PlantSpec;

public final class PlantFoodEffects {

    private PlantFoodEffects() {
    }

    public static PlantFoodEffect from(PlantSpec spec) {
        return PlantFoodGroup.effectFor(spec);
    }

    public static long durationTicks(PlantSpec spec) {
        return PlantFoodGroup.durationTicksFor(spec);
    }

    public static boolean supports(PlantSpec spec) {
        return PlantFoodGroup.supports(spec);
    }
}
