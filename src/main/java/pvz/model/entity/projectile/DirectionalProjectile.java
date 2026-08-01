package pvz.model.entity.projectile;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.plant.shooter.ShotVector;
import pvz.model.entity.zombie.Zombie;

public final class DirectionalProjectile extends Entity {
    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    private final ProjectileType type;
    private final ShotVector vector;

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

        this.x = tileCenter(startColumn);
        this.y = tileCenter(startRow);
        this.damage = damage;

        this.remainingDistance =
                rangeTiles == Integer.MAX_VALUE
                        ? Double.POSITIVE_INFINITY
                        : rangeTiles;
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

            world.board().damageTerrain(
                    column,
                    row,
                    type.damageAgainstTerrain(damage)
            );

            world.game().unregister(this);
            return;
        }

        Zombie zombie = world.board()
                .findZombieInTile(column, row);

        if (zombie != null) {
            type.hitZombie(zombie, damage, tick);
            world.game().unregister(this);
            return;
        }

        if (remainingDistance <= 0) {
            world.game().unregister(this);
        }
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
