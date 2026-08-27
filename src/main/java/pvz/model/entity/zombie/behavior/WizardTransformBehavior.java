package pvz.model.entity.zombie.behavior;

import java.util.HashSet;
import java.util.Set;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieAllegiance;

public final class WizardTransformBehavior implements ZombieBehavior {
    private final long castIntervalTicks;
    private final Set<Plant> transformedPlants = new HashSet<>();
    private long nextCastTick;

    public WizardTransformBehavior(double castIntervalSeconds) {
        castIntervalTicks = Math.max(
                1,
                (long) Math.ceil(castIntervalSeconds * Game.TICKS_PER_SECOND)
        );
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        nextCastTick = currentTick + castIntervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        transformedPlants.removeIf(Plant::isRemovedFromWorld);
        if (currentTick < nextCastTick) {
            return;
        }
        nextCastTick = currentTick + castIntervalTicks;
        Plant target = world.getTopPlants().stream()
                .filter(Plant::isZombieTargetable)
                .findFirst()
                .orElse(null);
        transform(target);
    }

    @Override
    public boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        transform(plant);
        return true;
    }

    private void transform(Plant plant) {
        if (plant == null || plant.isRemovedFromWorld()) {
            return;
        }
        if (!plant.addActionBlocker(this)) {
            return;
        }
        transformedPlants.add(plant);
        GameEvents.publish(plant.getName() + " was transformed into a cat.");
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
            restoreTransformedPlants();
        }
    }

    @Override
    public void onDeath(Zombie zombie, World world, long currentTick) {
        restoreTransformedPlants();
    }

    private void restoreTransformedPlants() {
        if (transformedPlants.isEmpty()) {
            return;
        }
        for (Plant plant : transformedPlants) {
            if (!plant.isRemovedFromWorld()) {
                plant.removeActionBlocker(this);
            }
        }
        transformedPlants.clear();
        GameEvents.publish("Wizard's transformed plants returned to normal.");
    }
}
