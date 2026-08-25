package pvz.model.entity.zombie.behavior;

import java.util.ArrayList;
import java.util.List;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantRemovalResult;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

public final class PushObstacleBehavior implements ZombieBehavior {
    private final String obstacleId;
    private final String obstacleName;
    private final double obstacleHealth;
    private final int obstacleCount;
    private final double spacingTiles;
    private final boolean blocksStraightProjectiles;
    private final boolean crushesPlants;
    private final boolean meltsOnFire;
    private final String spawnOnDestroyZombieId;
    private final int spawnOnDestroyCount;
    private final double spawnOnDestroySpacingTiles;
    private final List<PushedObstacle> obstacles = new ArrayList<>();

    public PushObstacleBehavior(
            String obstacleId,
            String obstacleName,
            double obstacleHealth,
            int obstacleCount,
            double spacingTiles,
            boolean blocksStraightProjectiles,
            boolean crushesPlants,
            boolean meltsOnFire,
            String spawnOnDestroyZombieId,
            int spawnOnDestroyCount,
            double spawnOnDestroySpacingTiles
    ) {
        this.obstacleId = obstacleId;
        this.obstacleName = obstacleName;
        this.obstacleHealth = obstacleHealth;
        this.obstacleCount = obstacleCount;
        this.spacingTiles = spacingTiles;
        this.blocksStraightProjectiles = blocksStraightProjectiles;
        this.crushesPlants = crushesPlants;
        this.meltsOnFire = meltsOnFire;
        this.spawnOnDestroyZombieId = spawnOnDestroyZombieId;
        this.spawnOnDestroyCount = spawnOnDestroyCount;
        this.spawnOnDestroySpacingTiles = spawnOnDestroySpacingTiles;
        validateDestructionSpawnParameters();
    }

    @Override
    public void onSpawn(Zombie zombie, World world, long currentTick) {
        for (int index = 0; index < obstacleCount; index++) {
            PushedObstacle obstacle = new PushedObstacle(
                    obstacleId,
                    obstacleName,
                    obstacleHealth,
                    blocksStraightProjectiles,
                    crushesPlants,
                    meltsOnFire,
                    destroyedObstacle -> spawnZombiesFromObstacle(
                            destroyedObstacle,
                            world
                    )
            );
            obstacle.spawn(world, zombie.getX(), zombie.getY());
            obstacles.add(obstacle);
        }
        synchronizeObstacles(zombie, world);
    }

    @Override
    public void onTick(Zombie zombie, World world, long currentTick) {
        synchronizeObstacles(zombie, world);
    }

    @Override
    public void onPositionChanged(
            Zombie zombie,
            World world,
            long currentTick
    ) {
        synchronizeObstacles(zombie, world);
    }

    private void synchronizeObstacles(Zombie zombie, World world) {
        for (int slotIndex = 0; slotIndex < obstacles.size(); slotIndex++) {
            PushedObstacle obstacle = obstacles.get(slotIndex);
            if (obstacle.isDead()) {
                continue;
            }
            obstacle.moveTo(
                    zombie.getX() - spacingTiles * (slotIndex + 1),
                    zombie.getY()
            );
            crushPlantAtObstacle(obstacle, world);
        }
    }

    private void spawnZombiesFromObstacle(
            PushedObstacle obstacle,
            World world
    ) {
        if (spawnOnDestroyZombieId == null) {
            return;
        }

        int column = Math.max(
                1,
                Math.min(world.board().getCols(), obstacle.getTileX())
        );
        int row = obstacle.getTileY();

        for (int index = 0; index < spawnOnDestroyCount; index++) {
            Zombie spawnedZombie = world.spawnZombie(
                    spawnOnDestroyZombieId,
                    column,
                    row
            );
            double targetX = Math.max(
                    0,
                    obstacle.getX()
                            - index * spawnOnDestroySpacingTiles
            );
            spawnedZombie.moveByTiles(targetX - spawnedZombie.getX());
        }

        GameEvents.publish(
                obstacle.getName() + " released "
                        + spawnOnDestroyCount + " "
                        + spawnOnDestroyZombieId + " zombies."
        );
    }

    private void validateDestructionSpawnParameters() {
        boolean hasZombieId = spawnOnDestroyZombieId != null;
        boolean hasSpawnValues = spawnOnDestroyCount > 0
                || spawnOnDestroySpacingTiles > 0;

        if (hasZombieId
                && (spawnOnDestroyCount <= 0
                || spawnOnDestroySpacingTiles <= 0)) {
            throw new IllegalArgumentException(
                    "obstacle destruction spawn requires a positive count "
                            + "and spacing"
            );
        }
        if (!hasZombieId && hasSpawnValues) {
            throw new IllegalArgumentException(
                    "obstacle destruction spawn values require a zombie id"
            );
        }
    }

    private void crushPlantAtObstacle(
            PushedObstacle obstacle,
            World world
    ) {
        if (!obstacle.crushesPlants()
                || !world.board().inBounds(
                obstacle.getTileX(),
                obstacle.getTileY()
        )) {
            return;
        }

        List<Plant> plants = world.board().getTile(
                obstacle.getTileX(),
                obstacle.getTileY()
        ).getPlants();
        for (int index = plants.size() - 1; index >= 0; index--) {
            Plant plant = plants.get(index);
            if (!plant.isZombieTargetable()) {
                continue;
            }
            PlantRemovalResult result = plant.tryRemove(
                    PlantThreat.INSTANT_DESTROY
            );
            if (result == PlantRemovalResult.REMOVED) {
                GameEvents.publish(
                        obstacle.getName() + " crushed " + plant.getName()
                                + " at (" + plant.getTileX() + ", "
                                + plant.getTileY() + ")."
                );
            }
            return;
        }
    }
}
