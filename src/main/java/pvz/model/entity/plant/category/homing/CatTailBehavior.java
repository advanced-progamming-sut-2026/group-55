package pvz.model.entity.plant.category.homing;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.plantfood.PlantFoodVolley;
import pvz.model.entity.projectile.homing.HomingTarget;

final class CatTailBehavior extends AbstractHomingBehavior {

    CatTailBehavior(Plant owner, HomingProfile profile) {
        super(owner, profile);
    }

    @Override
    protected boolean hasTarget(long currentTick) {
        return resolveTarget() != null;
    }

    @Override
    protected boolean fireOnce(long currentTick) {
        HomingTarget target = resolveTarget();

        if (target == null) {
            return false;
        }

        launch(target, new CatTailImpact(profile().damage()));

        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        PlantFoodVolley.start(
                world().game(),
                currentTick,
                profile().plantFoodProjectileCount(),
                profile().plantFoodProjectileIntervalTicks(),
                () -> !owner().isRemovedFromWorld(),
                step -> fireVolleyShot()
        );
    }

    private void fireVolleyShot() {
        HomingTarget target = resolveTarget();

        if (target == null) {
            return;
        }

        launch(target, new CatTailImpact(profile().damage()));
    }

    private HomingTarget resolveTarget() {
        if (HomingTargetResolver.hasHostileZombie(world())) {
            return HomingTargetResolver.catTailPriorityZombie(
                    world(),
                    owner()
            );
        }

        return HomingTargetResolver.catTailPriorityDestructibleTarget(
                world(),
                owner()
        );
    }
}
