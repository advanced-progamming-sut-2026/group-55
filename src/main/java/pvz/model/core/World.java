package pvz.model.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunCollectionOutcome;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;

public final class World {
    private static final int RADIOACTIVE_ZOMBIE_RADIUS = 2;
    private static final int RADIOACTIVE_PLANT_RADIUS = 1;

    private static final double RADIOACTIVE_ZOMBIE_DAMAGE = 150;
    private static final double RADIOACTIVE_PLANT_DAMAGE = 80;

    private final Game game;
    private final Board board;
    private final SunBank sunBank;

    private final List<Collectible> collectibles = new ArrayList<>();

    public World(Game game, Board board, SunBank sunBank) {
        this.game = Objects.requireNonNull(game);
        this.board = Objects.requireNonNull(board);
        this.sunBank = Objects.requireNonNull(sunBank);
    }

    public Game game() {
        return game;
    }

    public Board board() {
        return board;
    }

    public SunBank sunBank() {
        return sunBank;
    }

    public void addCollectible(Collectible collectible) {
        collectibles.add(Objects.requireNonNull(collectible));
    }

    public void removeCollectible(Collectible collectible) {
        collectibles.remove(collectible);
    }

    public List<Collectible> getCollectibles() {
        return List.copyOf(collectibles);
    }

    public SunCollectionOutcome collectSun(Sun sun) {
        Objects.requireNonNull(sun, "sun cannot be null");

        if (sun.isRemoved() || !collectibles.contains(sun)) {
            throw new IllegalStateException(
                    "sun is not available for collection"
            );
        }

        if (sun.isRadioactiveWhileFalling()) {
            explodeRadioactiveSun(
                    sun.getTileX(),
                    sun.getTileY()
            );

            sun.remove();
            return SunCollectionOutcome.EXPLODED;
        }

        sunBank.add(sun.getValue());
        sun.remove();
        return SunCollectionOutcome.COLLECTED;
    }

    private void explodeRadioactiveSun(
            int centerX,
            int centerY
    ) {
        damageZombiesInExplosion(centerX, centerY);
        damagePlantsInExplosion(centerX, centerY);
    }
    // mantegh zambie ha va plant haye mojood tooye yek square behtare bere tooye board
    private void damageZombiesInExplosion(
            int centerX,
            int centerY
    ) {
        for (Zombie zombie : board.getZombies()) {
            if (isInsideSquare(
                    zombie.getTileX(),
                    zombie.getTileY(),
                    centerX,
                    centerY,
                    RADIOACTIVE_ZOMBIE_RADIUS
            )) {
                zombie.takeDamage(
                        RADIOACTIVE_ZOMBIE_DAMAGE
                );
            }
        }
    }

    private void damagePlantsInExplosion(
            int centerX,
            int centerY
    ) {
        int minX = Math.max(
                1,
                centerX - RADIOACTIVE_PLANT_RADIUS
        );

        int maxX = Math.min(
                board.getCols(),
                centerX + RADIOACTIVE_PLANT_RADIUS
        );

        int minY = Math.max(
                1,
                centerY - RADIOACTIVE_PLANT_RADIUS
        );

        int maxY = Math.min(
                board.getRows(),
                centerY + RADIOACTIVE_PLANT_RADIUS
        );

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                damagePlantsInTile(x, y);
            }
        }
    }

    private void damagePlantsInTile(int x, int y) {
        Tile tile = board.getTile(x, y);

        for (Plant plant : tile.getPlants()) {
            plant.takeDamage(
                    RADIOACTIVE_PLANT_DAMAGE
            );
        }
    }

    private boolean isInsideSquare(
            int x,
            int y,
            int centerX,
            int centerY,
            int radius
    ) {
        return Math.abs(x - centerX) <= radius
                && Math.abs(y - centerY) <= radius;
    }
}
