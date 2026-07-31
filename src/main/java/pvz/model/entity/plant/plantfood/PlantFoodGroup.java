package pvz.model.entity.plant.plantfood;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

import pvz.model.core.Game;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.shooter.ShooterPlantFoodProfile;
import pvz.model.entity.plant.shooter.ShooterPlantFoodProfiles;

public enum PlantFoodGroup {

    SUN_PRODUCER(
            Set.of(
                    "sunflower",
                    "twin sunflower",
                    "primal sunflower",
                    "sun-shroom"
            ),
            Plant::applyBehaviorPlantFood,
            ignoredSpec ->
                    2L * Game.TICKS_PER_SECOND
    ),

    RAPID_SHOOTER(
            Set.of(
                    "peashooter"
            ),
            Plant::applyBehaviorPlantFood,
            PlantFoodGroup::shooterRequestedDurationTicks
    );

    private static final long MINIMUM_DURATION_TICKS = 2L * Game.TICKS_PER_SECOND;

    private static final Map<String, PlantFoodGroup> GROUP_BY_NAME = buildIndex();

    private final Set<String> plantNames;
    private final PlantFoodEffect effect;
    private final ToLongFunction<PlantSpec> durationProvider;

    PlantFoodGroup(
            Set<String> plantNames,
            PlantFoodEffect effect,
            ToLongFunction<PlantSpec> durationProvider
    ) {
        this.plantNames = Set.copyOf(plantNames);

        this.effect = effect;
        this.durationProvider = durationProvider;
    }

    public static PlantFoodEffect effectFor(PlantSpec spec) {
        PlantFoodGroup group = groupFor(spec);

        return group == null ? null : group.effect;
    }

    public static PlantFoodEffect effectFor(String rawName) {
        PlantFoodGroup group = groupFor(rawName);

        return group == null ? null : group.effect;
    }

    public static long durationTicksFor(PlantSpec spec) {
        PlantFoodGroup group = groupFor(spec);

        if (group == null) {
            return 0;
        }

        long requestedDuration = group.durationProvider.applyAsLong(spec);

        if (requestedDuration <= 0) {
            throw new IllegalStateException(
                    "plant food duration for " + spec.getName() + " must be positive");
        }

        return Math.max(MINIMUM_DURATION_TICKS, requestedDuration);
    }

    public static boolean supports(PlantSpec spec) {
        return groupFor(spec) != null;
    }

    private static PlantFoodGroup groupFor(PlantSpec spec) {
        if (spec == null) {
            return null;
        }

        return groupFor(spec.getName());
    }

    private static PlantFoodGroup groupFor(String rawName) {
        if (rawName == null) {
            return null;
        }

        return GROUP_BY_NAME.get(normalize(rawName));
    }

    private static long shooterRequestedDurationTicks(PlantSpec spec) {
        ShooterPlantFoodProfile profile = ShooterPlantFoodProfiles.from(spec);

        if (profile == null) {
            throw new IllegalStateException(
                    spec.getName() + " does not have a shooter plant food profile"
            );
        }

        return profile.durationTicks();
    }

    private static Map<String, PlantFoodGroup> buildIndex() {
        Map<String, PlantFoodGroup> groups = new HashMap<>();

        for (PlantFoodGroup group : values()) {
            for (String plantName : group.plantNames) {
                String normalizedName = normalize(plantName);

                PlantFoodGroup previous = groups.put(normalizedName, group);

                if (previous != null) {
                    throw new IllegalStateException(
                            plantName
                                    + " belongs to multiple plant food groups: "
                                    + previous
                                    + " and "
                                    + group
                    );
                }
            }
        }

        return Map.copyOf(groups);
    }

    private static String normalize(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
