package pvz.model.entity.plant.category.melee;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.DamageContext;

class AreaMeleeBehavior extends AbstractMeleeBehavior {

    AreaMeleeBehavior(Plant owner, MeleeProfile profile) {
        super(owner, profile);
    }

    @Override
    protected boolean hasAttackTarget(long currentTick) {
        int radius = profile().rangeTiles();
        return world().getHostileZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .anyMatch(zombie -> Math.abs(
                        zombie.getTileX() - column()
                ) <= radius && Math.abs(
                        zombie.getTileY() - row()
                ) <= radius);
    }

    @Override
    protected void performAttack(long currentTick) {
        world().damageZombiesInArea(
                column(),
                row(),
                profile().rangeTiles(),
                currentDamage(currentTick),
                DamageContext.AttackDelivery.CONTACT
        );
        markAttack(currentTick, MeleeAttackDirection.AREA);
        publishAttack("released a melee area attack.");
    }

    protected double currentDamage(long currentTick) {
        return profile().damage();
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        world().damageZombiesInArea(
                column(),
                row(),
                profile().plantFoodRadius(),
                currentDamage(currentTick)
                        * profile().plantFoodDamageMultiplier(),
                DamageContext.AttackDelivery.CONTACT
        );
        markAttack(currentTick, MeleeAttackDirection.AREA);
        publishAttack("used its plant food area attack.");
    }
}
