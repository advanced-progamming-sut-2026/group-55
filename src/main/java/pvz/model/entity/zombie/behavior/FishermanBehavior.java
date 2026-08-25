package pvz.model.entity.zombie.behavior;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;

public final class FishermanBehavior implements ZombieBehavior {
    private final int maximumRange;
    private final int pullDistanceTiles;
    private final int discardDistanceTiles;
    private final long castIntervalTicks;
    private long nextCastTick;

    public FishermanBehavior(
            int maximumRange,
            int pullDistanceTiles,
            int discardDistanceTiles,
            double castIntervalSeconds
    ) {
        this.maximumRange = maximumRange;
        this.pullDistanceTiles = pullDistanceTiles;
        this.discardDistanceTiles = discardDistanceTiles;
        this.castIntervalTicks = Math.max(
                1,
                (long) Math.ceil(castIntervalSeconds * Game.TICKS_PER_SECOND)
        );
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        nextCastTick = currentTick + castIntervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (currentTick < nextCastTick) {
            return;
        }
        nextCastTick = currentTick + castIntervalTicks;
        Plant target = world.findNearestPlantAhead(zombie, maximumRange);
        if (target == null) {
            return;
        }
        int distance = zombie.getTileX() - target.getTileX();
        if (distance <= discardDistanceTiles) {
            target.tryRemove(PlantThreat.INSTANT_DESTROY);
            GameEvents.publish("Fisherman tossed away " + target.getName() + ".");
            return;
        }
        if (target.tryRelocate(
                target.getTileX() + pullDistanceTiles,
                target.getTileY()
        )) {
            GameEvents.publish("Fisherman pulled " + target.getName()
                    + " " + pullDistanceTiles + " tile(s) to the right.");
        }
    }

    @Override
    public double modifyMovementMultiplier(
            Zombie zombie,
            World world,
            long currentTick,
            double multiplier
    ) {
        return 0;
    }
}
