package pvz.model.entity.zombie.behavior;

import pvz.model.core.board.TileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public final class SnorkelBehavior implements ZombieBehavior {
    @Override
    public DamageContext onIncomingHit(Zombie zombie, DamageContext context) {
        if (zombie.getWorld() == null
                || zombie.isEating()
                || zombie.getWorld().board().getTile(
                zombie.getTileX(), zombie.getRow()
        ).getType() != TileType.WATER) {
            return context;
        }
        return context.delivery() == DamageContext.AttackDelivery.LOBBED
                ? context
                : context.withDamage(0);
    }
}
