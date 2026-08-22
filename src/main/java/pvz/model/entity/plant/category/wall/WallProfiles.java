package pvz.model.entity.plant.category.wall;

import java.util.Locale;
import java.util.Objects;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

final class WallProfiles {

    private WallProfiles() {
    }

    static boolean blocksVaulting(PlantSpec spec) {
        requireWall(spec);

        return "tall-nut".equals(normalize(spec.getName()));
    }

    static boolean supportsArmorPlantFood(PlantSpec spec) {
        if (spec == null || spec.getCategory() != PlantCategory.WALL) {
            return false;
        }

        return switch (normalize(spec.getName())) {
            case "wall-nut",
                    "tall-nut",
                    "explode-o-nut",
                    "pumpkin" -> true;
            default -> false;
        };
    }

    private static void requireWall(PlantSpec spec) {
        Objects.requireNonNull(spec, "plant spec cannot be null");

        if (spec.getCategory() != PlantCategory.WALL) {
            throw new IllegalArgumentException(
                    spec.getName() + " is not a wall plant"
            );
        }
    }

    private static String normalize(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
