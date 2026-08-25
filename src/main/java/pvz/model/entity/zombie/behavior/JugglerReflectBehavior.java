package pvz.model.entity.zombie.behavior;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public final class JugglerReflectBehavior implements ZombieBehavior {
    private final double spinningSpeedMultiplier;
    private final long spinDurationTicks;
    private long spinningUntilTick;

    public JugglerReflectBehavior(
            double spinningSpeedMultiplier,
            double spinDurationSeconds
    ) {
        this.spinningSpeedMultiplier = spinningSpeedMultiplier;
        spinDurationTicks = Math.max(
                1,
                (long) Math.ceil(spinDurationSeconds * Game.TICKS_PER_SECOND)
        );
    }

    @Override
    public DamageContext onIncomingHit(Zombie zombie, DamageContext context) {
        if (context.projectileType() == null
                || context.delivery()
                != DamageContext.AttackDelivery.STRAIGHT
                || context.impactMode()
                != DamageContext.ImpactMode.SINGLE_TARGET) {
            return context;
        }
        if (zombie.isFrozen(context.tick())
                || zombie.isButtered(context.tick())) {
            return context;
        }
        spinningUntilTick = Math.max(
                spinningUntilTick,
                context.tick() + spinDurationTicks
        );
        World world = zombie.getWorld();
        Plant target = world.findNearestPlantForProjectileAhead(
                zombie,
                world.board().getCols()
        );
        if (target != null) {
            world.board().hitPlantWithReflectedProjectile(
                    target,
                    context.projectileType(),
                    context.damage()
            );
        }
        GameEvents.publish("Juggler reflected a straight projectile.");
        return context.withDamage(0);
    }

    @Override
    public double modifyMovementMultiplier(
            Zombie zombie,
            World world,
            long currentTick,
            double multiplier
    ) {
        return currentTick < spinningUntilTick
                ? multiplier * spinningSpeedMultiplier
                : multiplier;
    }
}
