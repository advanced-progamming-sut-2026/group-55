package pvz.model.entity.zombie;

import java.util.Objects;
import pvz.model.entity.projectile.ProjectileType;

public record DamageContext(
        double damage,
        DamageSource source,
        ProjectileType projectileType,
        AttackPath attackPath,
        boolean bypassArmor,
        long tick
) {
    public DamageContext {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        Objects.requireNonNull(source, "damage source cannot be null");
        Objects.requireNonNull(attackPath, "attack path cannot be null");
    }

    public DamageContext withDamage(double newDamage) {
        return new DamageContext(
                newDamage, source, projectileType, attackPath, bypassArmor, tick
        );
    }

    public enum DamageSource { PROJECTILE, DIRECT, POISON }
    public enum AttackPath { STRAIGHT, LOBBED, AREA, CONTACT, UNKNOWN }
}
