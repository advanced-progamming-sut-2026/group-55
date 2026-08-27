package pvz.model.entity.plant.projectile;

import java.util.Objects;
import java.util.Set;

import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.ProjectilePassThroughModifierCapability;
import pvz.model.entity.projectile.ProjectileModifierTarget;

public final class ProjectileModifierResolver {

    private ProjectileModifierResolver() {
    }

    public static void applyAtTile(
            World world,
            ProjectileModifierTarget projectile,
            Set<Plant> appliedModifiers,
            int column,
            int row
    ) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(projectile, "projectile cannot be null");
        Objects.requireNonNull(
                appliedModifiers,
                "applied modifiers cannot be null"
        );

        if (!world.board().inBounds(column, row)) {
            return;
        }

        for (Plant plant : world.board().getTile(column, row).getPlants()) {
            applyPlantModifier(world, projectile, appliedModifiers, plant);
        }
    }

    private static void applyPlantModifier(
            World world,
            ProjectileModifierTarget projectile,
            Set<Plant> appliedModifiers,
            Plant plant
    ) {
        if (plant.isRemovedFromWorld()
                || plant.isActionBlocked()
                || world.board().isPlantCovered(plant)
                || appliedModifiers.contains(plant)) {
            return;
        }

        ProjectilePassThroughModifierCapability modifier =
                plant.behaviorCapability(
                        ProjectilePassThroughModifierCapability.class
                );

        if (modifier == null) {
            return;
        }

        appliedModifiers.add(plant);
        modifier.modifyProjectile(projectile);
    }
}
