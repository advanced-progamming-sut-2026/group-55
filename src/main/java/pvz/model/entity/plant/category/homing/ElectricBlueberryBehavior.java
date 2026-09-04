package pvz.model.entity.plant.category.homing;

import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.projectile.homing.HomingTarget;

final class ElectricBlueberryBehavior extends AbstractHomingBehavior {

    ElectricBlueberryBehavior(Plant owner, HomingProfile profile) {
        super(owner, profile);
    }

    @Override
    protected boolean hasTarget(long currentTick) {
        return HomingTargetResolver.hasHostileZombie(world())
                || !HomingTargetResolver.destructibleTargets(world())
                        .isEmpty();
    }

    @Override
    protected boolean fireOnce(long currentTick) {
        HomingTarget target = resolveTarget();

        if (target == null) {
            return false;
        }

        strike(target);

        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        for (HomingTarget target : resolvePlantFoodTargets()) {
            strike(target);
        }
    }

    private List<HomingTarget> resolvePlantFoodTargets() {
        int count = profile().plantFoodTargetCount();

        if (HomingTargetResolver.hasHostileZombie(world())) {
            return HomingTargetResolver.randomDistinctHostileZombies(
                    world(),
                    count
            );
        }

        return HomingTargetResolver.randomDistinctDestructibleTargets(
                world(),
                count
        );
    }

    private HomingTarget resolveTarget() {
        if (HomingTargetResolver.hasHostileZombie(world())) {
            return profile().priorityTargeting()
                    ? HomingTargetResolver.highestThreatHostileZombie(world())
                    : HomingTargetResolver.randomHostileZombie(world());
        }

        return profile().priorityTargeting()
                ? HomingTargetResolver.highestThreatDestructibleTarget(world())
                : HomingTargetResolver.randomDestructibleTarget(world());
    }

    private void strike(HomingTarget target) {
        launch(target, new ElectricBlueberryImpact(profile().damage()));
    }
}
