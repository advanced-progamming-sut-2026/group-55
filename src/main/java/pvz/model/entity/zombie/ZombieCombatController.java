package pvz.model.entity.zombie;

import java.util.Objects;

import pvz.model.core.World;

/**
 * Symmetric contact combat between zombies on opposite allegiances.
 *
 * <p>This controller deliberately knows nothing about hypnosis or plants. Any
 * HOSTILE/ALLIED pair sharing a tile treats the other as an enemy, stops
 * movement and attacks through the regular armor-aware damage pipeline.</p>
 */
final class ZombieCombatController {

    private Zombie target;
    private long nextAttackTick = Long.MAX_VALUE;

    boolean update(
            Zombie self,
            Zombie enemy,
            long currentTick,
            double damage,
            long attackIntervalTicks
    ) {
        Objects.requireNonNull(self, "zombie cannot be null");
        Objects.requireNonNull(enemy, "enemy zombie cannot be null");

        if (enemy != target) {
            target = enemy;
            nextAttackTick = currentTick + attackIntervalTicks;
        }

        if (currentTick >= nextAttackTick) {
            enemy.takeZombieCombatDamage(damage, currentTick);
            nextAttackTick = currentTick + attackIntervalTicks;
        }

        return true;
    }

    void delayNextAttack(long currentTick, long attackIntervalTicks) {
        if (target == null || target.isDead()) {
            return;
        }
        nextAttackTick = Math.max(
                nextAttackTick,
                currentTick + attackIntervalTicks
        );
    }

    void accelerateNextAttack(long currentTick, long attackIntervalTicks) {
        if (target == null || target.isDead()) {
            return;
        }
        nextAttackTick = Math.min(
                nextAttackTick,
                currentTick + attackIntervalTicks
        );
    }

    boolean isFighting() {
        return target != null && !target.isDead();
    }

    void reset() {
        target = null;
        nextAttackTick = Long.MAX_VALUE;
    }
}
