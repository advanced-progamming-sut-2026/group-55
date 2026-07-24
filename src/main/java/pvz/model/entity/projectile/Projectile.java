package pvz.model.entity.projectile;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.zombie.Zombie;
import java.util.Objects;
import pvz.model.core.HorizontalDirection;

public class Projectile extends Entity {
    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    private final ProjectileType type;

    private final double terminalX;
    private final HorizontalDirection direction;

    protected double x;
    protected double y;

    public Projectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double damage,
            ProjectileType type,
            int rangeTiles
    ) {
        this(
                world,
                name,
                startColumn,
                startRow,
                damage,
                type,
                rangeTiles,
                HorizontalDirection.RIGHT
        );
    }
    public Projectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double damage,
            ProjectileType type,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "projectile range must be positive"
            );
        }

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

        this.direction = Objects.requireNonNull(
                direction,
                "projectile direction cannot be null"
        );

        this.x = tileCenter(startColumn);
        this.y = tileCenter(startRow);
        this.damage = damage;

        this.terminalX = calculateTerminalX(
                startColumn,
                rangeTiles
        );
    }

    public ProjectileType getType() {
        return type;
    }

    private double calculateTerminalX(int startColumn, int rangeTiles) {
        if (direction == HorizontalDirection.RIGHT) {
            return calculateRightTerminalX(startColumn, rangeTiles);
        }

        return calculateLeftTerminalX(startColumn, rangeTiles);
    }

    private double calculateRightTerminalX(int startColumn, int rangeTiles) {
        if (rangeTiles == Integer.MAX_VALUE) {
            return world.board().getCols();
        }

        return Math.min(world.board().getCols(), startColumn + rangeTiles);
    }

    private double calculateLeftTerminalX(int startColumn, int rangeTiles) {
        if (rangeTiles == Integer.MAX_VALUE) {
            return 0;
        }

        return Math.max(0, startColumn - rangeTiles - 1);
    }

    private double clampToTerminal(double candidateX) {
        if (direction == HorizontalDirection.RIGHT) {
            return Math.min(candidateX, terminalX);
        }

        return Math.max(candidateX, terminalX);
    }

    private boolean isBlockingTileFirst(Integer blockingTileColumn, Zombie zombie, double previousX) {
        if (blockingTileColumn == null) {
            return false;
        }

        if (zombie == null) {
            return true;
        }

        double tileHitX =
                direction == HorizontalDirection.RIGHT ? blockingTileColumn - 1.0 : blockingTileColumn;

        double tileDistance = Math.abs(tileHitX - previousX);

        double zombieDistance = Math.abs(zombie.getX() - previousX);

        return tileDistance <= zombieDistance;
    }

    private boolean hasReachedTerminal() {
        if (direction == HorizontalDirection.RIGHT) {
            return x >= terminalX;
        }

        return x <= terminalX;
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

        double movement = TILES_PER_SECOND / Game.TICKS_PER_SECOND * direction.sign();
        double nextX = clampToTerminal(x + movement);
        int row = getTileY();

        x = nextX;

        Integer blockingTileColumn = world.board().findHitBlockingTile(
                                            row,
                                            previousX,
                                            nextX
                                            );

        Zombie zombie = world.board().findHitZombie(
                            row,
                            previousX,
                            nextX
                            );

        boolean tileIsFirst = isBlockingTileFirst(blockingTileColumn, zombie, previousX);

        if (tileIsFirst) {
            world.board().damageTerrain(blockingTileColumn, row, damage);
            world.game().unregister(this);
            return;
        }

        if (zombie != null) {
            zombie.takeDamage(damage);
            world.game().unregister(this);
            return;
        }

        if (hasReachedTerminal()) {
            world.game().unregister(this);
        }
    }

    public HorizontalDirection getDirection() {
        return direction;
    }
}
