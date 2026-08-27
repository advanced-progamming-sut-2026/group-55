package pvz.model.entity.projectile;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

public final class DirectionalProjectile extends Entity {
    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    private final ProjectileType type;
    private final ShotVector vector;
    private final ProjectileHitLimit hitLimit;
    private final Set<Zombie> hitZombies = new HashSet<>();
    private final Set<PushedObstacle> hitObstacles = new HashSet<>();
    private final Set<Long> hitTerrainTiles = new HashSet<>();

    private double remainingDistance;
    private double x;
    private double y;

    public DirectionalProjectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double damage,
            ProjectileType type,
            int rangeTiles,
            ShotVector vector
    ) {
        this(
                world,
                name,
                startColumn,
                startRow,
                0,
                damage,
                type,
                rangeTiles,
                vector
        );
    }

    public DirectionalProjectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double spawnOffset,
            double damage,
            ProjectileType type,
            int rangeTiles,
            ShotVector vector
    ) {
        this(
                world,
                name,
                startColumn,
                startRow,
                spawnOffset,
                damage,
                type,
                rangeTiles,
                vector,
                ProjectileHitLimit.singleHit()
        );
    }

    public DirectionalProjectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double spawnOffset,
            double damage,
            ProjectileType type,
            int rangeTiles,
            ShotVector vector,
            ProjectileHitLimit hitLimit
    ) {
        if (damage < 0) {
            throw new IllegalArgumentException(
                    "projectile damage cannot be negative"
            );
        }

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "projectile range must be positive"
            );
        }

        if (!Double.isFinite(spawnOffset) || spawnOffset < 0) {
            throw new IllegalArgumentException(
                    "projectile spawn offset must be finite and non-negative"
            );
        }

        this.world = Objects.requireNonNull(
                world,
                "world cannot be null"
        );

        this.name = Objects.requireNonNull(
                name,
                "projectile name cannot be null"
        );

        this.type = Objects.requireNonNull(
                type,
                "projectile type cannot be null"
        );

        this.vector = Objects.requireNonNull(
                vector,
                "shot vector cannot be null"
        );

        this.hitLimit = Objects.requireNonNull(
                hitLimit,
                "projectile hit limit cannot be null"
        );

        this.x = tileCenter(startColumn)
                + spawnOffset * vector.unitColumnStep();
        this.y = tileCenter(startRow)
                + spawnOffset * vector.unitRowStep();
        this.damage = damage;

        this.remainingDistance =
                rangeTiles == Integer.MAX_VALUE
                        ? Double.POSITIVE_INFINITY
                        : Math.max(0, rangeTiles - spawnOffset);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void update(long tick) {
        double movement = Math.min(
                TILES_PER_SECOND / Game.TICKS_PER_SECOND,
                remainingDistance
        );

        double nextX =
                x + movement * vector.unitColumnStep();

        double nextY =
                y + movement * vector.unitRowStep();

        if (!isInsideBoard(nextX, nextY)) {
            world.game().unregister(this);
            return;
        }

        x = nextX;
        y = nextY;

        if (Double.isFinite(remainingDistance)) {
            remainingDistance -= movement;
        }

        int column = getTileX();
        int row = getTileY();

        if (world.board()
                .getTile(column, row)
                .blocksStraightProjectiles()) {

            long terrainKey = terrainKey(column, row);

            if (hitTerrainTiles.add(terrainKey)) {
                world.board().damageTerrainWithProjectile(
                        column,
                        row,
                        damage,
                        type
                );
            }

            if (hitLimit.isReachedBy(hitCount())) {
                world.game().unregister(this);
                return;
            }
        }

        PushedObstacle obstacle = world.findPushedObstacleInTile(
                column,
                row,
                hitObstacles
        );
        if (obstacle != null) {
            obstacle.takeProjectileDamage(type, damage);
            hitObstacles.add(obstacle);
            if (hitLimit.isReachedBy(hitCount())) {
                world.game().unregister(this);
                return;
            }
        }

        for (Zombie zombie : world.getHostileZombies()) {
            if (hitZombies.contains(zombie)
                    || zombie.getTileX() != column
                    || zombie.getTileY() != row) {
                continue;
            }

            type.hitZombie(zombie, damage, tick);
            hitZombies.add(zombie);

            if (hitLimit.isReachedBy(hitCount())) {
                world.game().unregister(this);
                return;
            }
        }

        if (remainingDistance <= 0) {
            world.game().unregister(this);
        }
    }

    private static long terrainKey(int column, int row) {
        return ((long) column << Integer.SIZE)
                ^ Integer.toUnsignedLong(row);
    }

    private int hitCount() {
        return hitZombies.size()
                + hitTerrainTiles.size()
                + hitObstacles.size();
    }

    private boolean isInsideBoard(
            double candidateX,
            double candidateY
    ) {
        return candidateX >= 0
                && candidateX < world.board().getCols()
                && candidateY >= 0
                && candidateY < world.board().getRows();
    }
}
