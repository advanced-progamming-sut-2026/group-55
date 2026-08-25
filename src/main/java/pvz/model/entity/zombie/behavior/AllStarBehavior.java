package pvz.model.entity.zombie.behavior;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;

public final class AllStarBehavior implements ZombieBehavior {
    private final double runningMultiplier;
    private final double walkingMultiplier;
    private boolean charged = true;

    public AllStarBehavior(double runningMultiplier, double walkingMultiplier) {
        this.runningMultiplier = runningMultiplier;
        this.walkingMultiplier = walkingMultiplier;
    }

    @Override
    public boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        if (!charged) {
            return false;
        }
        plant.tryRemove(PlantThreat.INSTANT_DESTROY);
        charged = false;
        GameEvents.publish("Allstar finished its charge in lane " + zombie.getRow() + ".");
        return true;
    }

    @Override
    public double modifyMovementMultiplier(
            Zombie zombie,
            World world,
            long currentTick,
            double multiplier
    ) {
        return multiplier * (charged ? runningMultiplier : walkingMultiplier);
    }
}
