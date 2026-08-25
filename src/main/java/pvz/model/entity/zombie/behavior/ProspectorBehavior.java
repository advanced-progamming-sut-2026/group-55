package pvz.model.entity.zombie.behavior;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public final class ProspectorBehavior implements ZombieBehavior {
    private final long launchDelayTicks;
    private final int launchTargetColumn;
    private long launchTick;
    private boolean extinguished;
    private boolean reversed;

    public ProspectorBehavior(
            double launchDelaySeconds,
            int launchTargetColumn
    ) {
        launchDelayTicks = Math.max(
                1,
                (long) Math.ceil(launchDelaySeconds * Game.TICKS_PER_SECOND)
        );
        this.launchTargetColumn = launchTargetColumn;
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        launchTick = currentTick + launchDelayTicks;
    }

    @Override
    public void onAcceptedHit(Zombie zombie, DamageContext context) {
        if (!reversed && context.projectileType() == ProjectileType.ICE) {
            extinguished = true;
            GameEvents.publish("Prospector dynamite was extinguished.");
        }
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (!extinguished && !reversed && currentTick >= launchTick) {
            zombie.moveToColumn(launchTargetColumn);
            reversed = true;
            GameEvents.publish("Prospector was launched to the house side and reversed direction.");
        }
    }

    @Override
    public double modifyMovementMultiplier(
            Zombie zombie,
            World world,
            long currentTick,
            double multiplier
    ) {
        if (!reversed) {
            return multiplier;
        }
        return -multiplier;
    }
}
