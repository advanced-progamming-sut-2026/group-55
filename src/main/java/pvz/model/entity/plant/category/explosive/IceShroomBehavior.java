package pvz.model.entity.plant.category.explosive;

import pvz.model.entity.plant.Plant;

final class IceShroomBehavior extends AbstractExplosiveBehavior {

    IceShroomBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    protected void afterPlaced() {
        triggerEffect(placedTick());
    }

    @Override
    protected void applyEffect(long currentTick) {
        ZombieFreezeSupport.freezeWholeLawn(
                world(),
                currentTick,
                profile().freezeDurationTicks()
        );

        publishEffect("froze every zombie on the lawn.");
    }
}
