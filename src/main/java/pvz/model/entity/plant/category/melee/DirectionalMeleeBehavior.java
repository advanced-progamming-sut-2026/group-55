package pvz.model.entity.plant.category.melee;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

final class DirectionalMeleeBehavior extends AbstractMeleeBehavior {

    private final boolean fireAttack;

    DirectionalMeleeBehavior(
            Plant owner,
            MeleeProfile profile,
            boolean fireAttack
    ) {
        super(owner, profile);
        this.fireAttack = fireAttack;
    }

    @Override
    protected boolean hasAttackTarget(long currentTick) {
        return target() != null;
    }

    @Override
    protected void performAttack(long currentTick) {
        Zombie target = target();
        if (target == null) {
            return;
        }

        MeleeAttackDirection direction = target.getX() < owner().getX()
                ? MeleeAttackDirection.BACKWARD
                : MeleeAttackDirection.FORWARD;

        boolean hit = target.takeAbilityDamage(
                profile().damage(),
                DamageContext.AttackDelivery.CONTACT,
                DamageContext.ImpactMode.SINGLE_TARGET
        );
        if (hit && fireAttack) {
            target.clearColdEffects(currentTick);
        }

        markAttack(currentTick, direction);
        publishAttack("struck a zombie " + direction.name().toLowerCase()
                + ".");
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        double damage = profile().damage()
                * profile().plantFoodDamageMultiplier();
        for (int hit = 0; hit < profile().plantFoodHitCount(); hit++) {
            applyPlantFoodAreaHit(currentTick, damage);
        }
        markAttack(currentTick, MeleeAttackDirection.AREA);
        publishAttack("used its plant food melee burst.");
    }

    private Zombie target() {
        return MeleeTargetResolver.nearestAround(
                owner(),
                world(),
                profile().rangeTiles()
        );
    }

    private void applyPlantFoodAreaHit(
            long currentTick,
            double damage
    ) {
        int radius = profile().plantFoodRadius();
        for (Zombie zombie : world().getHostileZombies()) {
            if (zombie.isDead()
                    || Math.abs(zombie.getTileX() - column()) > radius
                    || Math.abs(zombie.getTileY() - row()) > radius) {
                continue;
            }

            boolean accepted = zombie.takeAbilityDamage(
                    damage,
                    DamageContext.AttackDelivery.CONTACT,
                    DamageContext.ImpactMode.AREA
            );
            if (accepted && fireAttack) {
                zombie.clearColdEffects(currentTick);
            }
        }
    }
}
