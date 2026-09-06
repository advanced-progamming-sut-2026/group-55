package pvz.model.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.function.Function;

import pvz.model.core.board.Board;
import pvz.model.core.board.HorizontalDirection;
import pvz.model.core.board.TileType;
import pvz.model.entity.LawnMower;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.plantfood.PlantFood;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunCollectionOutcome;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantStackingRole;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieAllegiance;
import pvz.model.quest.QuestEvent;
import pvz.model.quest.QuestEventSink;

public final class World {
    private static final int RADIOACTIVE_ZOMBIE_RADIUS = 2;
    private static final double RADIOACTIVE_ZOMBIE_DAMAGE = 150;
    private static final int RADIOACTIVE_PLANT_RADIUS = 1;
    private static final double RADIOACTIVE_PLANT_DAMAGE = 80;
    private static final double GLOWING_ZOMBIE_CHANCE = 0.05;
    private static final double REWARD_DROP_CHANCE = 0.10;
    private static final int COIN_DROP_AMOUNT = 50;

    private final Game game;
    private final Board board;
    private final BattleResources resources;
    private final ZombieRegistry zombieRegistry = new ZombieRegistry();
    private final List<PushedObstacle> pushedObstacles = new ArrayList<>();
    private final List<LawnMower> lawnMowers = new ArrayList<>();
    private final List<Collectible> collectibles = new ArrayList<>();
    private final EnemyContentResolver enemyContentResolver =
            new EnemyContentResolver(this);
    private final RandomGenerator random;
    private Function<String, Zombie> zombieCreator;
    private Function<String, Plant> plantCreator;
    private ZombieDiscoveryListener zombieDiscoveryListener =
            ZombieDiscoveryListener.none();
    private QuestEventSink questEventSink = QuestEventSink.none();

    public World(Game game, Board board, BattleResources resources) {
        this(game, board, resources, new Random());
    }

    public World(
            Game game,
            Board board,
            BattleResources resources,
            RandomGenerator random
    ) {
        this.game = Objects.requireNonNull(game, "game cannot be null");
        this.board = Objects.requireNonNull(board, "board cannot be null");
        this.resources = Objects.requireNonNull(
                resources,
                "resources cannot be null"
        );
        this.random = Objects.requireNonNull(
                random,
                "random generator cannot be null"
        );

        board.setGroundOccupancy(this::hasPushedObstacleInTile);
        createLawnMowers();
    }

    public Game game() {
        return game;
    }

    public Board board() {
        return board;
    }

    public BattleResources resources() {
        return resources;
    }

    public SunBank sunBank() {
        return resources.sunBank();
    }

    public BattleWallet battleWallet() {
        return resources.battleWallet();
    }

    public void addCollectible(Collectible collectible) {
        collectibles.add(
                Objects.requireNonNull(
                        collectible,
                        "collectible cannot be null"
                )
        );
    }

    public void removeCollectible(Collectible collectible) {
        collectibles.remove(collectible);
    }

    public List<Collectible> getCollectibles() {
        return List.copyOf(collectibles);
    }

    public void addZombie(Zombie zombie) {
        Zombie checkedZombie = Objects.requireNonNull(
                zombie,
                "zombie cannot be null"
        );
        zombieRegistry.add(checkedZombie);
        zombieDiscoveryListener.onZombieDiscovered(
                checkedZombie.getSpec()
        );
    }

    public boolean rollGlowingZombie() {
        return random.nextDouble() < GLOWING_ZOMBIE_CHANCE;
    }

    public void removeZombie(Zombie zombie) {
        zombieRegistry.remove(zombie);
    }

    public void resolveZombieDeathDrops(Zombie zombie) {
        Objects.requireNonNull(zombie, "zombie cannot be null");

        if (zombie.isGlowing()) {
            dropPlantFood();
        }

        if (random.nextDouble() < REWARD_DROP_CHANCE) {
            dropPersistentReward();
        }
    }

    private void dropPlantFood() {
        if (resources.tryAddPlantFood()) {
            GameEvents.publish(
                    "The glowing zombie dropped a plant food; you have "
                            + resources.getPlantFoodCount()
                            + " plant foods now."
            );
            return;
        }

        GameEvents.publish(
                "The glowing zombie dropped a plant food, "
                        + "but the storage is full."
        );
    }

    private void dropPersistentReward() {
        switch (random.nextInt(3)) {
            case 0 -> {
                resources.battleWallet().addCoins(COIN_DROP_AMOUNT);
                GameEvents.publish(
                        "A zombie dropped 50 coins; you have collected "
                                + resources.battleWallet()
                                .getCollectedCoins()
                                + " stage coins now."
                );
            }
            case 1 -> {
                resources.battleWallet().addDiamonds(1);
                GameEvents.publish(
                        "A zombie dropped a diamond; you have collected "
                                + resources.battleWallet()
                                .getCollectedDiamonds()
                                + " stage diamonds now."
                );
            }
            case 2 -> {
                resources.addCollectedPot();
                GameEvents.publish(
                        "A zombie dropped a pot; you have collected "
                                + resources.getCollectedPotCount()
                                + " stage pots now."
                );
            }
            default -> throw new IllegalStateException(
                    "unsupported zombie reward roll"
            );
        }
    }

    /**
     * Returns every zombie regardless of allegiance. Gameplay code that means
     * "enemy of the player" should use {@link #getHostileZombies()} instead.
     */
    public List<Zombie> getZombies() {
        return zombieRegistry.snapshot();
    }

    public List<Zombie> getHostileZombies() {
        return zombieRegistry.hostileView();
    }

    public List<Zombie> getAlliedZombies() {
        return zombieRegistry.alliedView();
    }

    public boolean changeZombieAllegiance(
            Zombie zombie,
            ZombieAllegiance newAllegiance
    ) {
        Objects.requireNonNull(zombie, "zombie cannot be null");
        Objects.requireNonNull(
                newAllegiance,
                "zombie allegiance cannot be null"
        );
        if (!zombieRegistry.contains(zombie) || zombie.isDead()) {
            return false;
        }
        if (zombie.getAllegiance() == newAllegiance) {
            return false;
        }
        zombieRegistry.changeAllegiance(zombie, newAllegiance);
        return true;
    }

    public Zombie findOpposingZombieInTile(Zombie zombie) {
        return zombieRegistry.findOpposingZombieInTile(zombie);
    }

    public Zombie findOpposingZombieInTile(
            ZombieAllegiance allegiance,
            int column,
            int row
    ) {
        if (!board.inBounds(column, row)) {
            return null;
        }
        return zombieRegistry.findOpposingZombieInTile(
                Objects.requireNonNull(
                        allegiance,
                        "zombie allegiance cannot be null"
                ),
                column,
                row
        );
    }

    public void addPushedObstacle(PushedObstacle obstacle) {
        PushedObstacle checkedObstacle = Objects.requireNonNull(
                obstacle,
                "pushed obstacle cannot be null"
        );
        if (!pushedObstacles.contains(checkedObstacle)) {
            pushedObstacles.add(checkedObstacle);
        }
    }

    public void removePushedObstacle(PushedObstacle obstacle) {
        pushedObstacles.remove(obstacle);
    }

    public List<PushedObstacle> getPushedObstacles() {
        return List.copyOf(pushedObstacles);
    }

    public boolean hasPushedObstacleInTile(int column, int row) {
        return pushedObstacles.stream()
                .filter(obstacle -> !obstacle.isDead())
                .anyMatch(obstacle -> obstacle.getTileX() == column
                        && obstacle.getTileY() == row);
    }

    public PushedObstacle findHitPushedObstacle(
            int row,
            double fromX,
            double toX,
            Set<PushedObstacle> ignoredObstacles
    ) {
        Objects.requireNonNull(
                ignoredObstacles,
                "ignored obstacles cannot be null"
        );
        PushedObstacle nearest = null;
        for (PushedObstacle obstacle : pushedObstacles) {
            if (ignoredObstacles.contains(obstacle)
                    || obstacle.isDead()
                    || !obstacle.blocksStraightProjectiles()
                    || obstacle.getTileY() != row
                    || !isObstacleInsideSegment(obstacle, fromX, toX)) {
                continue;
            }
            if (nearest == null
                    || Math.abs(obstacle.getX() - fromX)
                    < Math.abs(nearest.getX() - fromX)) {
                nearest = obstacle;
            }
        }
        return nearest;
    }

    public PushedObstacle findPushedObstacleInTile(
            int column,
            int row,
            Set<PushedObstacle> ignoredObstacles
    ) {
        Objects.requireNonNull(
                ignoredObstacles,
                "ignored obstacles cannot be null"
        );
        board.getTile(column, row);
        return pushedObstacles.stream()
                .filter(obstacle -> !ignoredObstacles.contains(obstacle))
                .filter(obstacle -> !obstacle.isDead())
                .filter(PushedObstacle::blocksStraightProjectiles)
                .filter(obstacle -> obstacle.getTileX() == column)
                .filter(obstacle -> obstacle.getTileY() == row)
                .findFirst()
                .orElse(null);
    }

    public void damagePushedObstaclesWithProjectileInArea(
            int centerColumn,
            int centerRow,
            int radius,
            double baseDamage,
            ProjectileType projectileType
    ) {
        Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );
        validateObstacleArea(centerColumn, centerRow, radius, baseDamage);
        for (PushedObstacle obstacle : List.copyOf(pushedObstacles)) {
            if (isInsideSquare(obstacle, centerColumn, centerRow, radius)) {
                obstacle.takeProjectileDamage(projectileType, baseDamage);
            }
        }
    }

    public void damagePushedObstaclesDirectlyInArea(
            int centerColumn,
            int centerRow,
            int radius,
            double damage
    ) {
        validateObstacleArea(centerColumn, centerRow, radius, damage);
        for (PushedObstacle obstacle : List.copyOf(pushedObstacles)) {
            if (isInsideSquare(obstacle, centerColumn, centerRow, radius)) {
                obstacle.takeDirectDamage(damage);
            }
        }
    }

    public boolean hasEnemyContentAt(int column, int row) {
        return enemyContentResolver.hasEnemyContentAt(column, row);
    }

    public void damageZombiesInArea(
            int column,
            int row,
            int radius,
            double damage,
            DamageContext.AttackDelivery delivery
    ) {
        board.damageZombiesInArea(
                getHostileZombies(),
                column,
                row,
                radius,
                damage,
                delivery
        );
    }

    public void damageEnemyContentsInArea(
            int column,
            int row,
            int radius,
            double damage
    ) {
        enemyContentResolver.damageInArea(column, row, radius, damage);
    }

    public void damageEnemyContentsInRow(int row, double damage) {
        enemyContentResolver.damageInRow(row, damage);
    }

    public void damageAllEnemyContents(double damage) {
        enemyContentResolver.damageEverything(damage);
    }

    public void destroyFireVulnerableObstaclesInRow(int row) {
        enemyContentResolver.destroyFireVulnerableObstaclesInRow(row);
    }

    public void clearZombieColdEffectsInRow(
            int row,
            long currentTick
    ) {
        // Heating a row is a status/environment interaction, not enemy
        // damage. It may beneficially thaw an allied zombie as well.
        board.clearColdEffectsFromZombiesInRow(
                getZombies(),
                row,
                currentTick
        );
    }

    public void notifyHostilePresentAt(
            int column,
            int row,
            long currentTick
    ) {
        enemyContentResolver.notifyHostilePresentAt(
                column,
                row,
                currentTick
        );
    }

    public boolean hasStraightTargetAhead(
            int row,
            double fromX
    ) {
        return hasStraightTarget(
                row,
                fromX,
                Integer.MAX_VALUE,
                HorizontalDirection.RIGHT
        );
    }

    public boolean hasStraightTargetAhead(
            int row,
            double fromX,
            int rangeTiles
    ) {
        return hasStraightTarget(
                row,
                fromX,
                rangeTiles,
                HorizontalDirection.RIGHT
        );
    }

    public boolean hasStraightTarget(
            int row,
            double fromX,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        boolean boardTarget = board.hasStraightTarget(
                zombieRegistry.hostileView(),
                row,
                fromX,
                rangeTiles,
                direction
        );
        return boardTarget || hasPushedObstacleOnStraightPath(
                row,
                fromX,
                rangeTiles,
                direction
        );
    }

    public boolean hasDirectionalTarget(
            int startColumn,
            int startRow,
            int rangeTiles,
            ShotVector vector
    ) {
        boolean boardTarget = board.hasDirectionalTarget(
                zombieRegistry.hostileView(),
                startColumn,
                startRow,
                rangeTiles,
                vector
        );
        return boardTarget || hasPushedObstacleOnDirectionalPath(
                startColumn,
                startRow,
                rangeTiles,
                vector
        );
    }

    public boolean hasZombieAhead(
            int row,
            double fromX
    ) {
        return hasZombieAhead(
                row,
                fromX,
                Set.of()
        );
    }

    public boolean hasZombieAhead(
            int row,
            double fromX,
            Set<Zombie> ignoredZombies
    ) {
        return zombieRegistry.hasZombieAhead(
                row,
                fromX,
                ignoredZombies
        );
    }

    public Zombie findHitZombie(
            int row,
            double fromX,
            double toX
    ) {
        return findHitZombie(
                row,
                fromX,
                toX,
                Set.of()
        );
    }

    public Zombie findHitZombie(
            int row,
            double fromX,
            double toX,
            Set<Zombie> ignoredZombies
    ) {
        return board.findHitZombie(
                zombieRegistry.hostileView(),
                row,
                fromX,
                toX,
                ignoredZombies
        );
    }

    public Zombie findZombieInTile(
            int column,
            int row
    ) {
        board.getTile(column, row);
        return zombieRegistry.findZombieInTile(column, row);
    }

    public List<Plant> getPlants() {
        List<Plant> plants = new ArrayList<>();

        for (int column = 1; column <= board.getCols(); column++) {
            for (int row = 1; row <= board.getRows(); row++) {
                plants.addAll(
                        board.getTile(column, row).getPlants()
                );
            }
        }

        return List.copyOf(plants);
    }

    public List<Plant> getTopPlants() {
        List<Plant> plants = new ArrayList<>();

        for (int column = 1; column <= board.getCols(); column++) {
            for (int row = 1; row <= board.getRows(); row++) {
                Plant topPlant = board.getTopPlant(column, row);
                if (topPlant != null) {
                    plants.add(topPlant);
                }
            }
        }

        return List.copyOf(plants);
    }

    public void setPlantCreator(Function<String, Plant> plantCreator) {
        if (this.plantCreator != null) {
            throw new IllegalStateException(
                    "plant creator is already configured"
            );
        }
        this.plantCreator = Objects.requireNonNull(
                plantCreator,
                "plant creator cannot be null"
        );
    }

    public Plant spawnPlantFromAbility(
            String plantName,
            int column,
            int row
    ) {
        if (plantCreator == null) {
            throw new IllegalStateException(
                    "plant creator is not configured"
            );
        }

        Plant plant = plantCreator.apply(plantName);
        if (plant == null) {
            throw new IllegalArgumentException(
                    "unknown plant: " + plantName
            );
        }

        board.plant(column, row, plant);
        if (!board.inBounds(column, row)
                || !board.getTile(column, row).getPlants().contains(plant)) {
            return null;
        }

        plant.place(this, column, row, game.getCurrentTick());
        if (!plant.isRemovedFromWorld()) {
            game.register(plant);
        }
        return plant;
    }

    public void removePlantsUnsupportedByWaterPlatform(
            int column,
            int row
    ) {
        if (!board.inBounds(column, row)) {
            return;
        }

        var tile = board.getTile(column, row);
        if (tile.getType() != TileType.WATER
                || tile.getPlants().stream().anyMatch(
                        plant -> plant.getStackingRole()
                                == PlantStackingRole.WATER_PLATFORM
                )) {
            return;
        }

        for (Plant plant : tile.getPlants()) {
            if (!plant.hasTag(PlantTag.WATER)) {
                plant.tryRemove(PlantThreat.SUPPORT_LOSS);
            }
        }
    }

    public void setZombieCreator(Function<String, Zombie> zombieCreator) {
        if (this.zombieCreator != null) {
            throw new IllegalStateException("zombie creator is already configured");
        }
        this.zombieCreator = Objects.requireNonNull(
                zombieCreator,
                "zombie creator cannot be null"
        );
    }

    public void setZombieDiscoveryListener(
            ZombieDiscoveryListener listener
    ) {
        zombieDiscoveryListener = Objects.requireNonNull(
                listener,
                "zombie discovery listener cannot be null"
        );
    }

    public void setQuestEventSink(QuestEventSink sink) {
        questEventSink = Objects.requireNonNull(
                sink,
                "quest event sink cannot be null"
        );
    }

    public void publishQuestEvent(QuestEvent event) {
        questEventSink.publish(Objects.requireNonNull(
                event,
                "quest event cannot be null"
        ));
    }

    /**
     * Counts only hostile zombies as defeated enemies. Allied/hypnotized
     * zombies dying later must not advance hostile-kill quests.
     */
    public void recordZombieDefeated(Zombie zombie) {
        Zombie checked = Objects.requireNonNull(
                zombie,
                "zombie cannot be null"
        );
        if (!checked.isHostile()) {
            return;
        }
        publishQuestEvent(QuestEvent.zombieKilled(
                checked.getSpec().getId()
        ));
    }

    public Zombie spawnZombie(
            String zombieId,
            int column,
            int row
    ) {
        return spawnZombie(
                zombieId,
                column,
                row,
                ZombieAllegiance.HOSTILE
        );
    }

    public Zombie spawnZombie(
            String zombieId,
            int column,
            int row,
            ZombieAllegiance allegiance
    ) {
        Zombie zombie = createZombieForSpawn(zombieId);
        zombie.spawn(
                this,
                column,
                row,
                Objects.requireNonNull(
                        allegiance,
                        "zombie allegiance cannot be null"
                )
        );
        return zombie;
    }

    public Zombie spawnZombie(
            String zombieId,
            int column,
            int row,
            ZombieAllegiance allegiance,
            boolean glowing
    ) {
        Zombie zombie = createZombieForSpawn(zombieId);
        zombie.spawnWithGlowingState(
                this,
                column,
                row,
                Objects.requireNonNull(
                        allegiance,
                        "zombie allegiance cannot be null"
                ),
                glowing
        );
        return zombie;
    }

    private Zombie createZombieForSpawn(String zombieId) {
        if (zombieCreator == null) {
            throw new IllegalStateException("zombie creator is not configured");
        }

        Zombie zombie = zombieCreator.apply(zombieId);
        if (zombie == null) {
            throw new IllegalArgumentException("unknown zombie: " + zombieId);
        }

        return zombie;
    }

    public boolean rollChance(double probability) {
        if (!Double.isFinite(probability) || probability < 0 || probability > 1) {
            throw new IllegalArgumentException("probability must be between 0 and 1");
        }
        return random.nextDouble() < probability;
    }

    public double randomDouble() {
        return random.nextDouble();
    }

    public int randomInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("random bound must be positive");
        }
        return random.nextInt(bound);
    }

    public Plant findNearestPlantInRow(
            int row,
            double fromX,
            int maximumDistance
    ) {
        return getTopPlants().stream()
                .filter(Plant::isZombieTargetable)
                .filter(plant -> plant.getTileY() == row)
                .filter(plant -> Math.abs(plant.getX() - fromX)
                        <= maximumDistance)
                .min(Comparator.comparingDouble(
                        plant -> Math.abs(plant.getX() - fromX)
                ))
                .orElse(null);
    }

    public Plant findNearestPlantAhead(
            Zombie zombie,
            int maximumDistance
    ) {
        return getTopPlants().stream()
                .filter(Plant::isZombieTargetable)
                .filter(plant -> plant.getTileY() == zombie.getRow())
                .filter(plant -> plant.getX() <= zombie.getX())
                .filter(plant -> zombie.getX() - plant.getX()
                        <= maximumDistance)
                .max(Comparator.comparingDouble(Plant::getX))
                .orElse(null);
    }

    public boolean hasTargetablePlantInRadius(
            int centerColumn,
            int centerRow,
            int radius
    ) {
        board.getTile(centerColumn, centerRow);
        if (radius < 0) {
            throw new IllegalArgumentException(
                    "plant detection radius cannot be negative"
            );
        }
        return getTopPlants().stream()
                .filter(Plant::isZombieTargetable)
                .anyMatch(plant -> Math.max(
                        Math.abs(plant.getTileX() - centerColumn),
                        Math.abs(plant.getTileY() - centerRow)
                ) <= radius);
    }

    public Plant findNearestUncoveredPlantAhead(
            Zombie zombie,
            int maximumDistance
    ) {
        return getTopPlants().stream()
                .filter(Plant::isZombieTargetable)
                .filter(plant -> !board.isPlantCovered(plant))
                .filter(plant -> plant.getTileY() == zombie.getRow())
                .filter(plant -> plant.getX() <= zombie.getX())
                .filter(plant -> zombie.getX() - plant.getX()
                        <= maximumDistance)
                .max(Comparator.comparingDouble(Plant::getX))
                .orElse(null);
    }

    public Plant findNearestPlantForProjectileAhead(
            Zombie zombie,
            int maximumDistance
    ) {
        return getTopPlants().stream()
                .filter(plant -> !plant.isRemovedFromWorld())
                .filter(plant -> plant.getTileY() == zombie.getRow())
                .filter(plant -> plant.getX() <= zombie.getX())
                .filter(plant -> zombie.getX() - plant.getX()
                        <= maximumDistance)
                .max(Comparator.comparingDouble(Plant::getX))
                .orElse(null);
    }

    public void dropRecoveredSun(
            int value,
            double x,
            double y
    ) {
        if (value <= 0) {
            return;
        }
        Sun sun = Sun.recovered(this, x, y, value);
        addCollectible(sun);
        game.register(sun);
    }

    public SunCollectionOutcome collectSun(Sun sun) {
        Objects.requireNonNull(sun, "sun cannot be null");
        return collectSun(sun, sun.getTileX(), sun.getTileY());
    }

    /**
     * Collects a sun at the tile where the interaction happened.  The
     * interaction tile matters only while a radioactive sky sun is falling;
     * it becomes the center for both gameplay damage and the battle visual.
     */
    public SunCollectionOutcome collectSun(
            Sun sun,
            int collectionColumn,
            int collectionRow
    ) {
        Objects.requireNonNull(sun, "sun cannot be null");

        if (sun.isRemoved() || !collectibles.contains(sun)) {
            throw new IllegalStateException(
                    "sun is not available for collection"
            );
        }

        if (sun.isRadioactiveWhileFalling()) {
            requireSunCollectionTile(collectionColumn, collectionRow);
            explodeRadioactiveSunAt(
                    sun,
                    collectionColumn,
                    collectionRow
            );
            return SunCollectionOutcome.EXPLODED;
        }

        resources.sunBank().add(sun.getValue());
        sun.remove();
        return SunCollectionOutcome.COLLECTED;
    }

    private void requireSunCollectionTile(int column, int row) {
        if (!board.inBounds(column, row)) {
            throw new IllegalArgumentException(
                    "sun collection tile is out of bounds: ("
                            + column + ", " + row + ")"
            );
        }
    }

    private void explodeRadioactiveSunAt(
            Sun sun,
            int column,
            int row
    ) {
        board.damageZombiesDirectlyInArea(
                getZombies(),
                column,
                row,
                RADIOACTIVE_ZOMBIE_RADIUS,
                RADIOACTIVE_ZOMBIE_DAMAGE
        );
        board.damagePlantsInArea(
                column,
                row,
                RADIOACTIVE_PLANT_RADIUS,
                RADIOACTIVE_PLANT_DAMAGE
        );
        board.damageTilesInArea(
                column,
                row,
                RADIOACTIVE_ZOMBIE_RADIUS,
                RADIOACTIVE_ZOMBIE_DAMAGE
        );
        damagePushedObstaclesDirectlyInArea(
                column,
                row,
                RADIOACTIVE_ZOMBIE_RADIUS,
                RADIOACTIVE_ZOMBIE_DAMAGE
        );
        sun.remove();
    }

    public void collectPlantFood(PlantFood plantFood) {
        Objects.requireNonNull(
                plantFood,
                "plant food cannot be null"
        );

        if (!collectibles.contains(plantFood)) {
            throw new IllegalStateException(
                    "plant food is not available for collection"
            );
        }

        resources.tryAddPlantFood();
        plantFood.remove();
    }

    public void activateLawnMower(int row) {
        requireValidLawnMowerRow(row);
        lawnMowers.get(row - 1).activate();
    }

    public int eliminateAllZombies() {
        List<Zombie> zombies = getZombies();

        for (Zombie zombie : zombies) {
            zombie.takeDirectDamage(Double.MAX_VALUE);
        }

        return zombies.size();
    }

    public boolean isLawnMowerAvailable(int row) {
        if (!isValidLawnMowerRow(row)) {
            return false;
        }

        return !lawnMowers.get(row - 1).isUsed();
    }

    private void createLawnMowers() {
        for (int row = 1; row <= board.getRows(); row++) {
            lawnMowers.add(new LawnMower(this, row));
        }
    }

    private boolean isObstacleInsideSegment(
            PushedObstacle obstacle,
            double fromX,
            double toX
    ) {
        if (toX > fromX) {
            return obstacle.getX() > fromX && obstacle.getX() <= toX;
        }
        if (toX < fromX) {
            return obstacle.getX() < fromX && obstacle.getX() >= toX;
        }
        return false;
    }

    private boolean hasPushedObstacleOnStraightPath(
            int row,
            double fromX,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        return pushedObstacles.stream()
                .filter(obstacle -> !obstacle.isDead())
                .filter(PushedObstacle::blocksStraightProjectiles)
                .filter(obstacle -> obstacle.getTileY() == row)
                .anyMatch(obstacle -> isObstacleInDirection(
                        obstacle,
                        fromX,
                        rangeTiles,
                        direction
                ));
    }

    private boolean isObstacleInDirection(
            PushedObstacle obstacle,
            double fromX,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        double distance = (obstacle.getX() - fromX) * direction.sign();
        return distance >= 0
                && (rangeTiles == Integer.MAX_VALUE
                || distance <= rangeTiles);
    }

    private boolean hasPushedObstacleOnDirectionalPath(
            int startColumn,
            int startRow,
            int rangeTiles,
            ShotVector vector
    ) {
        return pushedObstacles.stream()
                .filter(obstacle -> !obstacle.isDead())
                .filter(PushedObstacle::blocksStraightProjectiles)
                .anyMatch(obstacle -> isObstacleOnDirectionalPath(
                        obstacle,
                        startColumn,
                        startRow,
                        rangeTiles,
                        vector
                ));
    }

    private boolean isObstacleOnDirectionalPath(
            PushedObstacle obstacle,
            int startColumn,
            int startRow,
            int rangeTiles,
            ShotVector vector
    ) {
        int columnDifference = obstacle.getTileX() - startColumn;
        int rowDifference = obstacle.getTileY() - startRow;
        return vector.reachesTile(columnDifference, rowDifference)
                && (rangeTiles == Integer.MAX_VALUE
                || Math.hypot(columnDifference, rowDifference)
                <= rangeTiles);
    }

    private void validateObstacleArea(
            int centerColumn,
            int centerRow,
            int radius,
            double damage
    ) {
        board.getTile(centerColumn, centerRow);
        if (radius < 0) {
            throw new IllegalArgumentException("area radius cannot be negative");
        }
        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException(
                    "area damage must be finite and non-negative"
            );
        }
    }

    private boolean isInsideSquare(
            PushedObstacle obstacle,
            int centerColumn,
            int centerRow,
            int radius
    ) {
        return Math.abs(obstacle.getTileX() - centerColumn) <= radius
                && Math.abs(obstacle.getTileY() - centerRow) <= radius;
    }

    private void requireValidLawnMowerRow(int row) {
        if (!isValidLawnMowerRow(row)) {
            throw new IllegalArgumentException(
                    "invalid lawn mower row: " + row
            );
        }
    }

    private boolean isValidLawnMowerRow(int row) {
        return row >= 1 && row <= lawnMowers.size();
    }
}
