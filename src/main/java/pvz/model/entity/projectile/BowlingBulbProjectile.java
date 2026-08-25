package pvz.model.entity.projectile;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.DamageContext;

public final class BowlingBulbProjectile
        extends Entity {

    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    private final ProjectileType type;
    private final Set<Zombie> hitZombies =
            new HashSet<>();
    private final Set<PushedObstacle> hitObstacles =
            new HashSet<>();
    private final int explosionRadius;

    private double x;
    private double y;
    private int targetRow;
    private int bounceDirection = 1;

    public BowlingBulbProjectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double damage,
            ProjectileType type
    ) {
        this(
                world,
                name,
                startColumn,
                startRow,
                damage,
                type,
                0
        );
    }

    private BowlingBulbProjectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double damage,
            ProjectileType type,
            int explosionRadius
    ) {
        if (damage < 0) {
            throw new IllegalArgumentException(
                    "projectile damage cannot be negative"
            );
        }

        if (explosionRadius < 0) {
            throw new IllegalArgumentException(
                    "explosion radius cannot be negative"
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

        this.x = tileCenter(startColumn);
        this.y = tileCenter(startRow);
        this.targetRow = startRow;
        this.damage = damage;
        this.explosionRadius = explosionRadius;
    }

    public static BowlingBulbProjectile explosive(
            World world,
            String name,
            int startColumn,
            int startRow,
            double damage,
            ProjectileType type,
            int explosionRadius
    ) {
        if (explosionRadius <= 0) {
            throw new IllegalArgumentException(
                    "explosive bulb needs a positive explosion radius"
            );
        }

        return new BowlingBulbProjectile(
                world,
                name,
                startColumn,
                startRow,
                damage,
                type,
                explosionRadius
        );
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
        double previousX = x;

        moveTowardTargetRow();

        double nextX = Math.min(
                world.board().getCols(),
                x + TILES_PER_SECOND
                        / Game.TICKS_PER_SECOND
        );

        x = nextX;

        int currentRow = getTileY();

        Integer blockingTileColumn =
                world.board().findHitBlockingTile(
                        currentRow,
                        previousX,
                        nextX
                );

        PushedObstacle obstacle = world.findHitPushedObstacle(
                currentRow,
                previousX,
                nextX,
                hitObstacles
        );

        Zombie zombie = world.findHitZombie(
                        currentRow,
                        previousX,
                        nextX,
                        hitZombies
                );

        if (isBlockingTileFirst(
                blockingTileColumn,
                obstacle,
                zombie,
                previousX
        )) {
            hitBlockingTile(
                    blockingTileColumn,
                    currentRow,
                    tick
            );
            world.game().unregister(this);
            return;
        }

        if (isObstacleFirst(obstacle, zombie, previousX)) {
            hitObstacle(obstacle, tick);
            world.game().unregister(this);
            return;
        }

        if (zombie != null) {
            hitZombie(zombie, tick);
            bounceToNextLane(currentRow);
        }

        if (x >= world.board().getCols()) {
            world.game().unregister(this);
        }
    }

    private void hitBlockingTile(
            int column,
            int row,
            long currentTick
    ) {
        if (isExplosive()) {
            explodeAt(column, row, currentTick);
            return;
        }

        world.board().damageTerrainWithProjectile(
                column,
                row,
                damage,
                type
        );
    }

    private void hitZombie(
            Zombie zombie,
            long currentTick
    ) {
        hitZombies.add(zombie);

        if (isExplosive()) {
            explodeAt(
                    zombie.getTileX(),
                    zombie.getTileY(),
                    currentTick
            );
            return;
        }

        type.hitZombie(zombie, damage, currentTick);
    }

    private void hitObstacle(
            PushedObstacle obstacle,
            long currentTick
    ) {
        hitObstacles.add(obstacle);
        if (isExplosive()) {
            explodeAt(
                    obstacle.getTileX(),
                    obstacle.getTileY(),
                    currentTick
            );
            return;
        }
        obstacle.takeProjectileDamage(type, damage);
    }

    private void explodeAt(
            int centerColumn,
            int centerRow,
            long currentTick
    ) {
        world.board().damageZombiesWithProjectileInArea(
                world.getZombies(),
                centerColumn,
                centerRow,
                explosionRadius,
                damage,
                type,
                DamageContext.AttackDelivery.STRAIGHT,
                currentTick
        );

        world.board().damageTilesWithProjectileInArea(
                centerColumn,
                centerRow,
                explosionRadius,
                damage,
                type
        );

        world.damagePushedObstaclesWithProjectileInArea(
                centerColumn,
                centerRow,
                explosionRadius,
                damage,
                type
        );
    }

    private boolean isExplosive() {
        return explosionRadius > 0;
    }

    private boolean isBlockingTileFirst(
            Integer blockingTileColumn,
            PushedObstacle obstacle,
            Zombie zombie,
            double previousX
    ) {
        if (blockingTileColumn == null) {
            return false;
        }

        double tileHitX =
                blockingTileColumn - 1.0;

        double tileDistance =
                Math.abs(tileHitX - previousX);

        double obstacleDistance = obstacle == null
                ? Double.POSITIVE_INFINITY
                : Math.abs(obstacle.getX() - previousX);

        double zombieDistance = zombie == null
                ? Double.POSITIVE_INFINITY
                : Math.abs(zombie.getX() - previousX);

        return tileDistance <= obstacleDistance
                && tileDistance <= zombieDistance;
    }

    private boolean isObstacleFirst(
            PushedObstacle obstacle,
            Zombie zombie,
            double previousX
    ) {
        if (obstacle == null) {
            return false;
        }
        return zombie == null
                || Math.abs(obstacle.getX() - previousX)
                <= Math.abs(zombie.getX() - previousX);
    }

    private void bounceToNextLane(int currentRow) {
        int upperRow = currentRow - 1;
        int lowerRow = currentRow + 1;

        boolean upperHasTarget =
                world.board().inBounds(1, upperRow)
                        && world.hasZombieAhead(
                                upperRow,
                                x,
                                hitZombies
                        );

        boolean lowerHasTarget =
                world.board().inBounds(1, lowerRow)
                        && world.hasZombieAhead(
                                lowerRow,
                                x,
                                hitZombies
                        );

        if (upperHasTarget && !lowerHasTarget) {
            moveToRow(upperRow);
            bounceDirection = -1;
            return;
        }

        if (lowerHasTarget && !upperHasTarget) {
            moveToRow(lowerRow);
            bounceDirection = 1;
            return;
        }

        int candidateRow =
                currentRow + bounceDirection;

        if (!world.board().inBounds(1, candidateRow)) {
            bounceDirection *= -1;
            candidateRow =
                    currentRow + bounceDirection;
        }

        if (world.board().inBounds(1, candidateRow)) {
            moveToRow(candidateRow);
        }
    }

    private void moveToRow(int newRow) {
        targetRow = newRow;
    }

    private void moveTowardTargetRow() {
        double targetY = tileCenter(targetRow);
        double maximumMovement =
                TILES_PER_SECOND / Game.TICKS_PER_SECOND;

        if (Math.abs(targetY - y) <= maximumMovement) {
            y = targetY;
            return;
        }

        y += Math.copySign(
                maximumMovement,
                targetY - y
        );
    }
}
