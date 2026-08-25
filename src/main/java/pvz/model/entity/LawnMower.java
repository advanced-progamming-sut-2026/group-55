package pvz.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

public final class LawnMower {
    private final World world;
    private final int row;
    private boolean used;

    public LawnMower(World world, int row) {
        this.world = Objects.requireNonNull(
                world,
                "world cannot be null"
        );

        if (row < 1 || row > world.board().getRows()) {
            throw new IllegalArgumentException(
                    "lawn mower row is out of bounds: " + row
            );
        }

        this.row = row;
    }

    public void activate() {
        if (used) {
            return;
        }

        List<String> killedZombies = new ArrayList<>();
        List<String> destroyedObstacles = new ArrayList<>();

        for (PushedObstacle obstacle
                : new ArrayList<>(world.getPushedObstacles())) {
            if (obstacle.getTileY() != row) {
                continue;
            }

            destroyedObstacles.add(obstacle.getName());
            obstacle.takeDirectDamage(Double.MAX_VALUE);
        }

        for (Zombie zombie : new ArrayList<>(world.getZombies())) {
            if (zombie.getTileY() != row) {
                continue;
            }

            killedZombies.add(zombie.getName());
            zombie.takeDamage(Double.MAX_VALUE);
        }

        used = true;

        GameEvents.publish(
                "The lawn mower in the row "
                        + row
                        + " is triggered and killed these zombies: "
                        + killedZombies
                        + "; destroyed these ground obstacles: "
                        + destroyedObstacles
        );
    }

    public boolean isUsed() {
        return used;
    }

    public int getRow() {
        return row;
    }
}
