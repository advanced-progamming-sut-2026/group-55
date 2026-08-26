package pvz.model.entity.plant.category.explosive;

import java.util.Objects;

import pvz.model.core.World;

final class ZombieFreezeSupport {

    private ZombieFreezeSupport() {
    }

    static void freezeTile(
            World world,
            int column,
            int row,
            long currentTick,
            long durationTicks
    ) {
        Objects.requireNonNull(world, "world cannot be null");

        world.board().freezeZombiesInArea(
                world.getZombies(),
                column,
                row,
                0,
                currentTick,
                durationTicks
        );
    }

    static void freezeWholeLawn(
            World world,
            long currentTick,
            long durationTicks
    ) {
        Objects.requireNonNull(world, "world cannot be null");

        for (int row = 1; row <= world.board().getRows(); row++) {
            world.board().freezeZombiesInRow(
                    world.getZombies(),
                    row,
                    currentTick,
                    durationTicks
            );
        }
    }
}
