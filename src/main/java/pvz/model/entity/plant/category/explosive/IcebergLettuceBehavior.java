package pvz.model.entity.plant.category.explosive;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.ContactTriggerCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.zombie.Zombie;

final class IcebergLettuceBehavior extends AbstractExplosiveBehavior
        implements ContactTriggerCapability, PlantFoodCapability {

    IcebergLettuceBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    public boolean tryTriggerOnHostileContact(long currentTick) {
        if (!canTrigger() || !hasZombieInTile()) {
            return false;
        }

        return triggerEffect(currentTick);
    }

    @Override
    protected void onIdleTick(long currentTick) {
        tryTriggerOnHostileContact(currentTick);
    }

    @Override
    protected boolean resolvesEffectImmediately() {
        return true;
    }

    @Override
    protected void applyEffect(long currentTick) {
        ZombieFreezeSupport.freezeTile(
                world(),
                column(),
                row(),
                currentTick,
                profile().freezeDurationTicks()
        );

        publishEffect("froze the zombies of its tile.");
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        ZombieFreezeSupport.freezeWholeLawn(
                world(),
                currentTick,
                profile().freezeDurationTicks()
        );
    }

    private boolean hasZombieInTile() {
        return world().getHostileZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .anyMatch(this::standsOnPlant);
    }

    private boolean standsOnPlant(Zombie zombie) {
        return zombie.getTileX() == column()
                && zombie.getTileY() == row();
    }
}
