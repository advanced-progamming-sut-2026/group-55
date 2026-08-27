package pvz.model.entity.plant.category.homing;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.projectile.homing.HomingTarget;

final class CaulipowerBehavior extends AbstractHomingBehavior {

    CaulipowerBehavior(Plant owner, HomingProfile profile) {
        super(owner, profile);
    }

    @Override
    protected boolean hasTarget(long currentTick) {
        return HomingTargetResolver.hasHostileZombie(world());
    }

    @Override
    protected boolean fireOnce(long currentTick) {
        HomingTarget target =
                HomingTargetResolver.randomHostileZombie(world());

        if (target == null) {
            return false;
        }

        launch(target, new HypnosisImpact());

        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        for (HomingTarget target
                : HomingTargetResolver.randomDistinctHostileZombies(
                        world(),
                        profile().plantFoodTargetCount()
                )) {
            launch(target, new HypnosisImpact());
        }
    }
}
