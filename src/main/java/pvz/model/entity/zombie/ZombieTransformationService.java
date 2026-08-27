package pvz.model.entity.zombie;

import java.util.Objects;

import pvz.model.core.GameEvents;
import pvz.model.core.World;

/**
 * Replaces a live zombie with another zombie type without treating the
 * replacement as a death. The original zombie leaves the active registry with
 * no drops or death event, while the replacement starts as a fresh entity.
 */
public final class ZombieTransformationService {

    private static final String GARGANTUAR_ID = "ZombieGargantuar";

    private ZombieTransformationService() {
    }

    public static Zombie transformToAlliedGargantuar(
            Zombie zombie,
            long currentTick
    ) {
        Objects.requireNonNull(zombie, "zombie cannot be null");

        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        World world = zombie.getWorld();
        if (world == null || zombie.isDead() || !zombie.isHostile()) {
            return null;
        }

        double x = zombie.getX();
        double y = zombie.getY();
        boolean glowing = zombie.isGlowing();

        if (!world.changeZombieAllegiance(
                zombie,
                ZombieAllegiance.ALLIED
        )) {
            return null;
        }

        zombie.detachForTransformation();

        Zombie gargantuar = world.spawnZombie(
                GARGANTUAR_ID,
                zombie.getTileX(),
                zombie.getTileY(),
                ZombieAllegiance.ALLIED,
                glowing
        );
        gargantuar.restorePositionAfterTransformation(x, y);

        GameEvents.publish(
                "Zombie at (" + gargantuar.getTileX()
                        + ", " + gargantuar.getTileY()
                        + ") transformed into an allied Gargantuar."
        );

        return gargantuar;
    }
}
