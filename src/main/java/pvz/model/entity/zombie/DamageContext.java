package pvz.model.entity.zombie;

import java.util.Objects;
import pvz.model.entity.projectile.ProjectileType;

public record DamageContext(
        double damage,
        DamageSource source,
        ProjectileType projectileType,
        AttackDelivery delivery,
        ImpactMode impactMode,
        boolean bypassArmor,
        long tick
) {
    public DamageContext {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        Objects.requireNonNull(source, "damage source cannot be null");
        Objects.requireNonNull(delivery, "attack delivery cannot be null");
        Objects.requireNonNull(impactMode, "impact mode cannot be null");
    }

    public DamageContext withDamage(double newDamage) {
        return new DamageContext(
                newDamage,
                source,
                projectileType,
                delivery,
                impactMode,
                bypassArmor,
                tick
        );
    }

    public enum DamageSource { PROJECTILE, ABILITY, DIRECT, POISON, ZOMBIE }
    public enum AttackDelivery { STRAIGHT, LOBBED, HOMING, CONTACT, UNKNOWN }
    public enum ImpactMode { SINGLE_TARGET, AREA }
}
