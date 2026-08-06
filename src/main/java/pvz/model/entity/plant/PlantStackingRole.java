package pvz.model.entity.plant;

import java.util.Set;

public enum PlantStackingRole {
    NONE,
    WATER_PLATFORM,
    SELF_STACKING,
    PROTECTIVE_COVER;

    static PlantStackingRole from(
            PlantCategory category,
            Set<PlantTag> tags
    ) {
        if (!tags.contains(PlantTag.STACK)) {
            return NONE;
        }

        if (tags.contains(PlantTag.WATER)) {
            return WATER_PLATFORM;
        }

        if (category == PlantCategory.SHOOTER) {
            return SELF_STACKING;
        }

        if (category == PlantCategory.WALL) {
            return PROTECTIVE_COVER;
        }

        throw new IllegalArgumentException(
                "unsupported STACK plant category: " + category
        );
    }
}
