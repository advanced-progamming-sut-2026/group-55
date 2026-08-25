package pvz.model.entity.zombie.behavior;

import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public final class LobbedShieldBehavior implements ZombieBehavior {
    @Override
    public DamageContext onIncomingHit(Zombie zombie, DamageContext context) {
        if (context.delivery() == DamageContext.AttackDelivery.LOBBED) {
            return context.withDamage(0);
        }
        return context;
    }
}
