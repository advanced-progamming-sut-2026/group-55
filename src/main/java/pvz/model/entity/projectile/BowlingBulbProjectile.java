package pvz.model.entity.projectile;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.zombie.Zombie;

public final class BowlingBulbProjectile
        extends Entity {

    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    private final ProjectileType type;
    private final Set<Zombie> hitZombies =
            new HashSet<>();

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
        if (damage < 0) {
            throw new IllegalArgumentException(
                    "projectile damage cannot be negative"
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

        Zombie zombie = world.board()
                .findHitZombie(
                        currentRow,
                        previousX,
                        nextX,
                        hitZombies
                );

        if (isBlockingTileFirst(
                blockingTileColumn,
                zombie,
                previousX
        )) {
            world.board().damageTerrain(
                    blockingTileColumn,
                    currentRow,
                    type.damageAgainstTerrain(damage)
            );

            world.game().unregister(this);
            return;
        }

        if (zombie != null) {
            type.hitZombie(zombie, damage, tick);
            hitZombies.add(zombie);
            bounceToNextLane(currentRow);
        }

        if (x >= world.board().getCols()) {
            world.game().unregister(this);
        }
    }

    private boolean isBlockingTileFirst(
            Integer blockingTileColumn,
            Zombie zombie,
            double previousX
    ) {
        if (blockingTileColumn == null) {
            return false;
        }

        if (zombie == null) {
            return true;
        }

        double tileHitX =
                blockingTileColumn - 1.0;

        double tileDistance =
                Math.abs(tileHitX - previousX);

        double zombieDistance =
                Math.abs(zombie.getX() - previousX);

        return tileDistance <= zombieDistance;
    }

    private void bounceToNextLane(int currentRow) {
        int upperRow = currentRow - 1;
        int lowerRow = currentRow + 1;

        boolean upperHasTarget =
                world.board().inBounds(1, upperRow)
                        && world.board().hasZombieAhead(
                                upperRow,
                                x,
                                hitZombies
                        );

        boolean lowerHasTarget =
                world.board().inBounds(1, lowerRow)
                        && world.board().hasZombieAhead(
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
