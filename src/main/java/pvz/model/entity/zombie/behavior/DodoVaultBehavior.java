package pvz.model.entity.zombie.behavior;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.zombie.Zombie;

public final class DodoVaultBehavior implements ZombieBehavior {
    private final int minimumVaultTiles;
    private final int maximumVaultTiles;
    private final int minimumObstacleHealth;

    public DodoVaultBehavior(
            int minimumVaultTiles,
            int maximumVaultTiles,
            int minimumObstacleHealth
    ) {
        this.minimumVaultTiles = minimumVaultTiles;
        this.maximumVaultTiles = maximumVaultTiles;
        this.minimumObstacleHealth = minimumObstacleHealth;
        if (minimumVaultTiles > maximumVaultTiles) {
            throw new IllegalArgumentException(
                    "minimum vault distance cannot exceed maximum"
            );
        }
    }

    @Override
    public boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        if (plant.blocksVaulting() || !isObstacle(plant)) {
            return false;
        }
        int possibleDistances = maximumVaultTiles - minimumVaultTiles + 1;
        int tiles = minimumVaultTiles + world.randomInt(possibleDistances);
        zombie.moveByTiles(-tiles);
        GameEvents.publish("Dodo vaulted " + tiles + " tile(s) over "
                + plant.getName() + ".");
        return true;
    }

    private boolean isObstacle(Plant plant) {
        return plant.getSpec().getBaseHp() >= minimumObstacleHealth
                || plant.hasTag(PlantTag.TRAP)
                || plant.hasTag(PlantTag.MOVE_ZOMBIES);
    }
}
