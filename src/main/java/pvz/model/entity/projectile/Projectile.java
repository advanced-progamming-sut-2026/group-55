package pvz.model.entity.projectile;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.zombie.Zombie;

public class Projectile extends Entity {
    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    private final ProjectileType type;

    private final double maximumX;

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
        if (rangeTiles <= 0) {
            throw new IllegalArgumentException("projectile range must be positive");
        }

        this.world = world;
        this.name = name;
        this.x = tileCenter(startColumn);
        this.y = tileCenter(startRow);
        this.damage = damage;
        this.type = type;
        this.maximumX = calculateMaximumX(startColumn, rangeTiles);
    }

    public ProjectileType getType() {
        return type;
    }

    private double calculateMaximumX(int startColumn, int rangeTiles) {
        if (rangeTiles == Integer.MAX_VALUE) {
            return world.board().getCols();
        }

        int lastReachableColumn = Math.min(
                world.board().getCols(),
                startColumn + rangeTiles
        );

        return lastReachableColumn;
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
        double nextX = Math.min(x + TILES_PER_SECOND / Game.TICKS_PER_SECOND, maximumX);
        int row = getTileY();

        Zombie zombie = world.board().findHitZombie(row, previousX, nextX);
        Integer blockingTileColumn = world.board().findHitBlockingTile(row, previousX, nextX);

        x = nextX;

        boolean tileIsFirst = blockingTileColumn != null
                && (zombie == null || blockingTileColumn - 1.0 <= zombie.getX());

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

        if (x >= maximumX) {
            world.game().unregister(this);
        }
    }
}
