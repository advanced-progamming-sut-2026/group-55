package pvz.model.entity.zombie.behavior;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;

public final class GargantuarBehavior implements ZombieBehavior {
    private final double throwHealthRatio;
    private final int impTargetColumn;
    private final String impZombieId;
    private boolean impThrown;

    public GargantuarBehavior(
            double throwHealthRatio,
            int impTargetColumn,
            String impZombieId
    ) {
        this.throwHealthRatio = throwHealthRatio;
        this.impTargetColumn = impTargetColumn;
        this.impZombieId = impZombieId;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        if (impThrown || zombie.getHealthRatio() > throwHealthRatio) {
            return;
        }
        world.spawnZombie(impZombieId, impTargetColumn, zombie.getRow());
        impThrown = true;
        GameEvents.publish("Gargantuar threw an Imp into column "
                + impTargetColumn + " of lane " + zombie.getRow() + ".");
    }

    @Override
    public boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        plant.tryRemove(PlantThreat.INSTANT_DESTROY);
        return true;
    }
}
