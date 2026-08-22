package pvz.model.entity.plant.category.shooter.bowlingbulb;

import java.util.Locale;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

public record BowlingBulbPlantFoodProfile(
        int totalBulbs,
        double damagePerBulb,
        int explosionRadius,
        long ticksBetweenBulbs
) {
    private static final int PLANT_FOOD_BULB_COUNT = 3;
    private static final double PLANT_FOOD_BULB_DAMAGE = 600;
    private static final int PLANT_FOOD_EXPLOSION_RADIUS = 1;
    private static final long PLANT_FOOD_SHOT_GAP_TICKS = 1;

    public BowlingBulbPlantFoodProfile {
        if (totalBulbs <= 0) {
            throw new IllegalArgumentException(
                    "plant food bulb count must be positive"
            );
        }

        if (!Double.isFinite(damagePerBulb)
                || damagePerBulb < 0) {
            throw new IllegalArgumentException(
                    "plant food bulb damage must be finite and non-negative"
            );
        }

        if (explosionRadius < 0) {
            throw new IllegalArgumentException(
                    "plant food explosion radius cannot be negative"
            );
        }

        if (totalBulbs > 1 && ticksBetweenBulbs <= 0) {
            throw new IllegalArgumentException(
                    "multi-bulb plant food needs a positive shot gap"
            );
        }
    }

    public static boolean supports(PlantSpec spec) {
        if (spec == null
                || spec.getCategory() != PlantCategory.SHOOTER) {
            return false;
        }

        return spec.getName()
                .strip()
                .toLowerCase(Locale.ROOT)
                .equals("bowling bulb");
    }

    public static BowlingBulbPlantFoodProfile from(
            PlantSpec spec
    ) {
        if (!supports(spec)) {
            throw new IllegalArgumentException(
                    "plant spec is not Bowling Bulb"
            );
        }

        return new BowlingBulbPlantFoodProfile(
                PLANT_FOOD_BULB_COUNT,
                PLANT_FOOD_BULB_DAMAGE,
                PLANT_FOOD_EXPLOSION_RADIUS,
                PLANT_FOOD_SHOT_GAP_TICKS
        );
    }
}
