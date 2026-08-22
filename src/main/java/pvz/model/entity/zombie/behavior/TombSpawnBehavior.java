package pvz.model.entity.zombie.behavior;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Tile;
import pvz.model.core.board.TileType;
import pvz.model.entity.zombie.Zombie;

public final class TombSpawnBehavior implements ZombieBehavior {
    private final long intervalTicks;
    private final int tombsPerCast;
    private long nextCastTick = Long.MAX_VALUE;

    public TombSpawnBehavior(int intervalSeconds, int tombsPerCast) {
        this.intervalTicks = (long) intervalSeconds * Game.TICKS_PER_SECOND;
        this.tombsPerCast = tombsPerCast;
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        nextCastTick = currentTick + intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (currentTick < nextCastTick) {
            return;
        }
        placeTombstones(world);
        nextCastTick = currentTick + intervalTicks;
    }

    private void placeTombstones(World world) {
        List<Point> candidates = new ArrayList<>();

        for (int column = 1; column <= world.board().getCols(); column++) {
            for (int row = 1; row <= world.board().getRows(); row++) {
                Tile tile = world.board().getTile(column, row);
                if (tile.getType() == TileType.NORMAL
                        && tile.getPlants().isEmpty()
                        && world.findZombieInTile(column, row) == null) {
                    candidates.add(new Point(column, row));
                }
            }
        }

        Collections.shuffle(candidates);
        int placed = 0;
        for (Point point : candidates) {
            if (world.board().placeTombstone(point.x, point.y)) {
                placed++;
            }
            if (placed == tombsPerCast) {
                return;
            }
        }
    }
}
