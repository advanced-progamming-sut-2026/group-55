package pvz.model.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.board.Board;
import pvz.model.core.board.HorizontalDirection;
import pvz.model.entity.LawnMower;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.plantfood.PlantFood;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunCollectionOutcome;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.zombie.Zombie;

public final class World {
    private static final int RADIOACTIVE_ZOMBIE_RADIUS = 2;
    private static final double RADIOACTIVE_ZOMBIE_DAMAGE = 150;
    private static final int RADIOACTIVE_PLANT_RADIUS = 1;
    private static final double RADIOACTIVE_PLANT_DAMAGE = 80;

    private final Game game;
    private final Board board;
    private final BattleResources resources;
    private final ZombieRegistry zombieRegistry = new ZombieRegistry();
    private final List<LawnMower> lawnMowers = new ArrayList<>();
    private final List<Collectible> collectibles = new ArrayList<>();

    public World(Game game, Board board, BattleResources resources) {
        this.game = Objects.requireNonNull(game, "game cannot be null");
        this.board = Objects.requireNonNull(board, "board cannot be null");
        this.resources = Objects.requireNonNull(
                resources,
                "resources cannot be null"
        );

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
        zombieRegistry.add(zombie);
    }

    public void removeZombie(Zombie zombie) {
        zombieRegistry.remove(zombie);
    }

    public List<Zombie> getZombies() {
        return zombieRegistry.snapshot();
    }

    public boolean hasStraightTargetAhead(
            int row,
            double fromX
    ) {
        return board.hasStraightTargetAhead(
                zombieRegistry.view(),
                row,
                fromX
        );
    }

    public boolean hasStraightTargetAhead(
            int row,
            double fromX,
            int rangeTiles
    ) {
        return board.hasStraightTargetAhead(
                zombieRegistry.view(),
                row,
                fromX,
                rangeTiles
        );
    }

    public boolean hasStraightTarget(
            int row,
            double fromX,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        return board.hasStraightTarget(
                zombieRegistry.view(),
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
        return board.hasDirectionalTarget(
                zombieRegistry.view(),
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
                zombieRegistry.view(),
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

    public SunCollectionOutcome collectSun(Sun sun) {
        Objects.requireNonNull(sun, "sun cannot be null");

        if (sun.isRemoved() || !collectibles.contains(sun)) {
            throw new IllegalStateException(
                    "sun is not available for collection"
            );
        }

        if (sun.isRadioactiveWhileFalling()) {
            board.damageZombiesDirectlyInArea(
                    getZombies(),
                    sun.getTileX(),
                    sun.getTileY(),
                    RADIOACTIVE_ZOMBIE_RADIUS,
                    RADIOACTIVE_ZOMBIE_DAMAGE
            );

            board.damagePlantsInArea(
                    sun.getTileX(),
                    sun.getTileY(),
                    RADIOACTIVE_PLANT_RADIUS,
                    RADIOACTIVE_PLANT_DAMAGE
            );

            board.damageTilesInArea(
                    sun.getTileX(),
                    sun.getTileY(),
                    RADIOACTIVE_ZOMBIE_RADIUS,
                    RADIOACTIVE_ZOMBIE_DAMAGE
            );

            sun.remove();
            return SunCollectionOutcome.EXPLODED;
        }

        resources.sunBank().add(sun.getValue());
        sun.remove();
        return SunCollectionOutcome.COLLECTED;
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
