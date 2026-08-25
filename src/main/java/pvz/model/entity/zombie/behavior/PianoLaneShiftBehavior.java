package pvz.model.entity.zombie.behavior;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.zombie.Zombie;

public final class PianoLaneShiftBehavior implements ZombieBehavior {
    private final long shiftIntervalTicks;
    private final int laneShiftTiles;
    private long nextShiftTick;

    public PianoLaneShiftBehavior(
            double shiftIntervalSeconds,
            int laneShiftTiles
    ) {
        shiftIntervalTicks = Math.max(
                1,
                (long) Math.ceil(shiftIntervalSeconds * Game.TICKS_PER_SECOND)
        );
        this.laneShiftTiles = laneShiftTiles;
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        nextShiftTick = currentTick + shiftIntervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (currentTick < nextShiftTick) {
            return;
        }
        nextShiftTick = currentTick + shiftIntervalTicks;
        for (Zombie target : world.getZombies()) {
            if (target == zombie || target.isDead()) {
                continue;
            }
            int direction = world.randomInt(2) == 0
                    ? -laneShiftTiles
                    : laneShiftTiles;
            int newRow = target.getRow() + direction;
            if (newRow < 1 || newRow > world.board().getRows()) {
                newRow = target.getRow() - direction;
            }
            if (newRow >= 1 && newRow <= world.board().getRows()) {
                target.moveToRow(newRow);
            }
        }
        GameEvents.publish("Piano music shuffled adjacent zombie lanes.");
    }
}
