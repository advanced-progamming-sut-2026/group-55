package pvz.model.entity.zombie.behavior;

import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public final class FireResistanceBehavior implements ZombieBehavior {
    @Override
    public DamageContext onIncomingHit(Zombie zombie, DamageContext context) {
        if (context.projectileType() == ProjectileType.FIRE) {
            return context.withDamage(0);
        }
        return context;
    }
}
