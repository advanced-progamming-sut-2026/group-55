package pvz.model.entity.zombie;

import java.util.Objects;

import pvz.model.core.GameEvents;

/**
 * General hypnosis mechanism. Any plant that turns a zombie to the player's
 * side must go through this service so the rules stay in one place.
 */
public final class HypnosisService {

    private HypnosisService() {
    }

    public static boolean hypnotize(Zombie zombie, long currentTick) {
        Objects.requireNonNull(zombie, "zombie cannot be null");

        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (zombie.isDead() || zombie.isAllied() || zombie.getWorld() == null) {
            return false;
        }

        if (!zombie.getWorld().changeZombieAllegiance(
                zombie,
                ZombieAllegiance.ALLIED
        )) {
            return false;
        }

        GameEvents.publish(
                "Zombie of type " + zombie.getName()
                        + " at (" + zombie.getTileX()
                        + ", " + zombie.getTileY()
                        + ") is hypnotized and fights for you now."
        );

        return true;
    }
}
