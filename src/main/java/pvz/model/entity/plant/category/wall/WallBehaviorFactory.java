package pvz.model.entity.plant.category.wall;

import java.util.Objects;
import java.util.Set;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.behavior.PlantBehavior;

public final class WallBehaviorFactory {

    private WallBehaviorFactory() {
    }

    public static PlantBehavior create(Plant owner, PlantSpec spec) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        Set<PlantTag> tags = spec.getTags();

        boolean blocksVaulting = tags.contains(PlantTag.BLOCK_VAULT);

        PlantBehavior taggedBehavior = createTaggedBehavior(
                owner,
                spec,
                blocksVaulting
        );

        if (taggedBehavior != null) {
            return taggedBehavior;
        }

        if (supportsPlantFood(spec)) {
            return new ArmoredWallBehavior(
                    owner,
                    blocksVaulting,
                    spec.getBaseHp()
            );
        }

        return new WallBehavior(
                owner,
                blocksVaulting
        );
    }

    public static PlantBehavior createBasicWallLike(
            Plant owner,
            PlantSpec spec
    ) {
        Objects.requireNonNull(owner, "owner plant cannot be null");
        Objects.requireNonNull(spec, "plant spec cannot be null");

        if (spec.getBaseHp() <= 0) {
            throw new IllegalArgumentException(
                    "basic wall health must be positive"
            );
        }

        if (spec.hasPlantFoodEffect()) {
            return new ArmoredWallBehavior(
                    owner,
                    false,
                    spec.getBaseHp()
            );
        }

        return new WallBehavior(owner, false);
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return spec != null
                && spec.getCategory() == PlantCategory.WALL
                && spec.getBaseHp() > 0
                && spec.hasPlantFoodEffect();
    }

    private static PlantBehavior createTaggedBehavior(
            Plant owner,
            PlantSpec spec,
            boolean blocksVaulting
    ) {
        Set<PlantTag> tags = spec.getTags();

        if (tags.contains(PlantTag.EXPLOSIVE)) {
            return new ExplodeONutBehavior(
                    owner,
                    blocksVaulting,
                    spec.getBaseHp(),
                    parseDamage(spec)
            );
        }

        if (tags.contains(PlantTag.REFLECT_DAMAGE)) {
            return new EndurianBehavior(
                    owner,
                    blocksVaulting,
                    spec.getBaseHp(),
                    parseDamage(spec)
            );
        }

        if (tags.contains(PlantTag.REPEL_ZOMBIES)) {
            return new GarlicBehavior(owner, blocksVaulting);
        }

        if (tags.contains(PlantTag.ATTRACT_ZOMBIES)) {
            return new SweetPotatoBehavior(owner, blocksVaulting);
        }

        if (tags.contains(PlantTag.SUN)) {
            return new SunBeanBehavior(
                    owner,
                    blocksVaulting,
                    spec.getBaseHp()
            );
        }

        return null;
    }

    private static double parseDamage(PlantSpec spec) {
        try {
            double damage = Double.parseDouble(
                    spec.getDamage().strip()
            );

            if (damage <= 0) {
                throw new IllegalArgumentException(
                        "wall damage must be positive"
                );
            }

            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid damage value for "
                            + spec.getName()
                            + ": "
                            + spec.getDamage(),
                    exception
            );
        }
    }
}
