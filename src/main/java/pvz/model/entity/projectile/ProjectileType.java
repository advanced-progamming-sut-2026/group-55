package pvz.model.entity.projectile;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.DamageContext;

public enum ProjectileType {
    NORMAL,
    FIRE,
    ICE,
    POISON;

    private static final double FIRE_DAMAGE_MULTIPLIER = 2;

    private static final long CHILL_DURATION_TICKS =
            10L * Game.TICKS_PER_SECOND;

    private static final long POISON_DURATION_TICKS =
            8L * Game.TICKS_PER_SECOND;

    private static final double POISON_DAMAGE_PER_SECOND = 3;

    private static final int MAXIMUM_POISON_STACKS = 5;

    public void hitZombie(
            Zombie zombie,
            double baseDamage,
            long currentTick
    ) {
        Objects.requireNonNull(
                zombie,
                "zombie cannot be null"
        );

        switch (this) {
            case NORMAL ->
                    hit(zombie, baseDamage, currentTick, false);

            case FIRE -> {
                zombie.removeChill(currentTick);
                zombie.removeFreeze(currentTick);
                hit(zombie, calculateDamage(baseDamage), currentTick, false);
            }

            case ICE -> {
                hit(zombie, baseDamage, currentTick, false);

                if (!zombie.isDead()) {
                    zombie.applyChill(
                            currentTick,
                            CHILL_DURATION_TICKS
                    );
                }
            }

            case POISON -> {
                hit(zombie, baseDamage, currentTick, true);

                if (!zombie.isDead()) {
                    zombie.applyPoison(
                            currentTick,
                            POISON_DURATION_TICKS,
                            POISON_DAMAGE_PER_SECOND,
                            MAXIMUM_POISON_STACKS
                    );
                }
            }
        }
    }

    private void hit(
            Zombie zombie,
            double damage,
            long currentTick,
            boolean bypassArmor
    ) {
        zombie.receiveHit(new DamageContext(
                damage,
                bypassArmor
                        ? DamageContext.DamageSource.POISON
                        : DamageContext.DamageSource.PROJECTILE,
                this,
                DamageContext.AttackPath.STRAIGHT,
                bypassArmor,
                currentTick
        ));
    }

    public double damageAgainstTerrain(
            double baseDamage
    ) {
        return calculateDamage(baseDamage);
    }

    public double calculateDamage(
            double baseDamage
    ) {
        if (this == FIRE) {
            return baseDamage * FIRE_DAMAGE_MULTIPLIER;
        }

        return baseDamage;
    }
}
