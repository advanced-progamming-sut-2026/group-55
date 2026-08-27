package pvz.model.entity.zombie.behavior;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunState;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieAllegiance;

public final class RaSunStealBehavior implements ZombieBehavior {
    private final int maximumStolenSun;
    private int stolenSun;

    public RaSunStealBehavior(int maximumStolenSun) {
        this.maximumStolenSun = maximumStolenSun;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        for (Collectible collectible : world.getCollectibles()) {
            if (!(collectible instanceof Sun sun)
                    || sun.getState() != SunState.AVAILABLE
                    || stolenSun >= maximumStolenSun) {
                continue;
            }
            int accepted = sun.consumeUpTo(
                    maximumStolenSun - stolenSun
            );
            stolenSun += accepted;
            if (accepted > 0) {
                GameEvents.publish("Ra stole " + accepted + " sun.");
            }
        }
    }

    @Override
    public void onAllegianceChanged(
            Zombie zombie,
            World world,
            ZombieAllegiance oldAllegiance,
            ZombieAllegiance newAllegiance,
            long currentTick
    ) {
        if (newAllegiance == ZombieAllegiance.ALLIED) {
            returnStolenSun(world);
        }
    }

    @Override
    public void onDeath(Zombie zombie, World world, long currentTick) {
        returnStolenSun(world);
    }

    private void returnStolenSun(World world) {
        if (stolenSun <= 0) {
            return;
        }
        int returned = stolenSun;
        stolenSun = 0;
        world.sunBank().add(returned);
        GameEvents.publish("Ra returned " + returned + " stolen sun.");
    }
}
