package pvz.model.entity.zombie.behavior;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;

public final class OctopusThrowBehavior implements ZombieBehavior {
    private final int rangeTiles;
    private final long attackIntervalTicks;
    private long nextAttackTick;

    public OctopusThrowBehavior(int rangeTiles, double attackIntervalSeconds) {
        this.rangeTiles = rangeTiles;
        this.attackIntervalTicks = Math.max(
                1,
                (long) Math.ceil(attackIntervalSeconds * Game.TICKS_PER_SECOND)
        );
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        nextAttackTick = currentTick + attackIntervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (currentTick < nextAttackTick) {
            return;
        }
        nextAttackTick = currentTick + attackIntervalTicks;
        Plant target = world.findNearestUncoveredPlantAhead(
                zombie,
                rangeTiles
        );
        if (target == null) {
            return;
        }
        if (world.board().coverPlantWithOctopus(target)) {
            GameEvents.publish(
                    "Octopus Zombie covered " + target.getName() + "."
            );
        }
    }
}
