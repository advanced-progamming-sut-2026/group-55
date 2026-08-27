package pvz.model.entity.projectile;

import java.util.Objects;

import pvz.model.core.board.ElementInteractionResolver;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.DamageContext;

public enum ProjectileType {
    NORMAL,
    ELECTRIC,
    FIRE,
    ICE,
    POISON;

    private static final double FIRE_DAMAGE_MULTIPLIER = 2;

    public boolean hitZombie(
            Zombie zombie,
            double baseDamage,
            long currentTick
    ) {
        return hitZombie(
                zombie,
                baseDamage,
                currentTick,
                DamageContext.AttackDelivery.STRAIGHT,
                DamageContext.ImpactMode.SINGLE_TARGET
        );
    }

    public boolean hitZombie(
            Zombie zombie,
            double baseDamage,
            long currentTick,
            DamageContext.AttackDelivery delivery
    ) {
        return hitZombie(
                zombie,
                baseDamage,
                currentTick,
                delivery,
                DamageContext.ImpactMode.SINGLE_TARGET
        );
    }

    public boolean hitZombie(
            Zombie zombie,
            double baseDamage,
            long currentTick,
            DamageContext.AttackDelivery delivery,
            DamageContext.ImpactMode impactMode
    ) {
        Objects.requireNonNull(
                zombie,
                "zombie cannot be null"
        );

        boolean bypassArmor = this == POISON;
        boolean accepted = hit(
                zombie,
                calculateDamage(baseDamage),
                currentTick,
                delivery,
                impactMode,
                bypassArmor
        );

        if (accepted) {
            ElementInteractionResolver.applyAcceptedZombieHit(
                    this,
                    zombie,
                    currentTick
            );
        }

        return accepted;
    }

    private boolean hit(
            Zombie zombie,
            double damage,
            long currentTick,
            DamageContext.AttackDelivery delivery,
            DamageContext.ImpactMode impactMode,
            boolean bypassArmor
    ) {
        return zombie.receiveHit(new DamageContext(
                damage,
                bypassArmor
                        ? DamageContext.DamageSource.POISON
                        : DamageContext.DamageSource.PROJECTILE,
                this,
                delivery,
                impactMode,
                bypassArmor,
                currentTick
        ));
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
