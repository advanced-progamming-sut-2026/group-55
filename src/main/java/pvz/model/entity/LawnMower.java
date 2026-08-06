package pvz.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.zombie.Zombie;

public final class LawnMower {

    private final World world;
    private final int row;

    private boolean used;


    public LawnMower(World world, int row) {
        this.world = Objects.requireNonNull(world);
        this.row = row;
        this.used = false;
    }


    public void activate() {

        if (used) {
            return;
        }

        List<String> killedZombies = new ArrayList<>();

        for (Zombie zombie : new ArrayList<>(world.board().getZombies())) {

            if (zombie.getTileY() == row) {

                killedZombies.add(zombie.getName());

                zombie.takeDamage(Double.MAX_VALUE);
            }
        }

        used = true;


        GameEvents.publish(
                "The lawn mower in the row "
                        + row
                        + " is triggered and killed these zombies: "
                        + killedZombies
        );
    }


    public boolean isUsed() {
        return used;
    }


    public int getRow() {
        return row;
    }
}
