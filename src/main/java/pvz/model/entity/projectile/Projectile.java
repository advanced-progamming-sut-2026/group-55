package pvz.model.entity.projectile;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.Game;
import pvz.model.core.board.HorizontalDirection;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.projectile.ProjectileModifierResolver;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

public class Projectile extends Entity implements ProjectileModifierTarget {
    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final double damage;
    private ProjectileType type;
    private final ProjectileFamily projectileFamily;
    private PeaHeatState peaHeatState;
    private double peaDamageMultiplier;
    private final ProjectileHitLimit hitLimit;
    private final Set<Zombie> hitZombies =
            new HashSet<>();
    private final Set<PushedObstacle> hitObstacles =
            new HashSet<>();
    private final Set<Integer> hitTerrainColumns =
            new HashSet<>();
    private final Set<Plant> appliedModifiers = new HashSet<>();

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
        this(
                world,
                name,
                startColumn,
                startRow,
                0,
                damage,
                type,
                rangeTiles,
                direction,
                ProjectileHitLimit.singleHit()
        );
    }

    public Projectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double spawnOffsetX,
            double damage,
            ProjectileType type,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        this(
                world,
                name,
                startColumn,
                startRow,
                spawnOffsetX,
                damage,
                type,
                rangeTiles,
                direction,
                ProjectileHitLimit.singleHit()
        );
    }

    public Projectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double spawnOffsetX,
            double damage,
            ProjectileType type,
            int rangeTiles,
            HorizontalDirection direction,
            ProjectileHitLimit hitLimit
    ) {
        this(
                world,
                name,
                startColumn,
                startRow,
                spawnOffsetX,
                damage,
                type,
                rangeTiles,
                direction,
                hitLimit,
                ProjectileFamily.GENERIC
        );
    }

    public Projectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            double spawnOffsetX,
            double damage,
            ProjectileType type,
            int rangeTiles,
            HorizontalDirection direction,
            ProjectileHitLimit hitLimit,
            ProjectileFamily projectileFamily
    ) {
        if (rangeTiles <= 0) {
            throw new IllegalArgumentException("projectile range must be positive");
        }

        if (damage < 0) {
            throw new IllegalArgumentException("projectile damage cannot be negative");
        }

        if (!Double.isFinite(spawnOffsetX)) {
            throw new IllegalArgumentException("projectile spawn offset must be finite");
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
        this.projectileFamily = Objects.requireNonNull(
                projectileFamily,
                "projectile family cannot be null"
        );
        initializePeaState();

        this.direction = Objects.requireNonNull(
                direction,
                "projectile direction cannot be null"
        );

        this.hitLimit = Objects.requireNonNull(
                hitLimit,
                "projectile hit limit cannot be null"
        );

        this.x = calculateStartX(startColumn, spawnOffsetX);
        this.y = tileCenter(startRow);
        this.damage = damage;

        this.terminalX = calculateTerminalX(
                startColumn,
                rangeTiles
        );
    }

    private double calculateStartX(int startColumn, double spawnOffsetX) {
        double startX = tileCenter(startColumn) + spawnOffsetX;

        return Math.max(
                0,
                Math.min(world.board().getCols(), startX)
        );
    }

    public ProjectileType getType() {
        return type;
    }

    @Override
    public ProjectileFamily getProjectileFamily() {
        return projectileFamily;
    }

    @Override
    public ProjectileType getProjectileType() {
        return type;
    }

    @Override
    public PeaHeatState getPeaHeatState() {
        return peaHeatState;
    }

    @Override
    public double getPeaDamageMultiplier() {
        return peaDamageMultiplier;
    }

    @Override
    public boolean promotePeaHeat(
            PeaHeatState newState,
            double totalDamageMultiplier
    ) {
        Objects.requireNonNull(newState, "pea heat state cannot be null");
        if (!Double.isFinite(totalDamageMultiplier)
                || totalDamageMultiplier <= 0) {
            throw new IllegalArgumentException(
                    "pea damage multiplier must be positive and finite"
            );
        }
        if (projectileFamily != ProjectileFamily.PEA
                || newState.ordinal() < peaHeatState.ordinal()
                || (newState == peaHeatState
                && totalDamageMultiplier <= peaDamageMultiplier)) {
            return false;
        }

        peaHeatState = newState;
        peaDamageMultiplier = totalDamageMultiplier;
        type = ProjectileType.FIRE;
        return true;
    }

    private void initializePeaState() {
        if (projectileFamily == ProjectileFamily.PEA
                && type == ProjectileType.FIRE) {
            peaHeatState = PeaHeatState.FIRE;
            peaDamageMultiplier = type.calculateDamage(1);
            return;
        }

        peaHeatState = PeaHeatState.UNHEATED;
        peaDamageMultiplier = 1;
    }

    private double effectiveBaseDamage() {
        if (projectileFamily != ProjectileFamily.PEA) {
            return damage;
        }

        double typeMultiplier = type.calculateDamage(1);
        return damage * peaDamageMultiplier / typeMultiplier;
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

    private boolean isBlockingTileFirst(
            Integer blockingTileColumn,
            PushedObstacle obstacle,
            Zombie zombie,
            double previousX
    ) {
        if (blockingTileColumn == null) {
            return false;
        }
        double tileHitX = direction == HorizontalDirection.RIGHT
                ? blockingTileColumn - 1.0
                : blockingTileColumn;
        double tileDistance = Math.abs(tileHitX - previousX);
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

    private int hitCount() {
        return hitZombies.size()
                + hitTerrainColumns.size()
                + hitObstacles.size();
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

        ProjectileModifierResolver.applyAtTile(
                world,
                this,
                appliedModifiers,
                getTileX(),
                row
        );

        // TODO: The resolver returns only the first blocking tile in the
        // movement segment. When a projectile moves from one already-hit
        // blocking tile directly into another, the second tile may be
        // skipped for this tick.

        while (true) {
            Integer blockingTileColumn = world.board().findHitBlockingTile(row, previousX, nextX);

            PushedObstacle obstacle = world.findHitPushedObstacle(
                    row,
                    previousX,
                    nextX,
                    hitObstacles
            );

            Zombie zombie = world.findHitZombie(row, previousX, nextX, hitZombies);

            if (isBlockingTileFirst(
                    blockingTileColumn,
                    obstacle,
                    zombie,
                    previousX
            )
                    && hitTerrainColumns.add(blockingTileColumn)) {

                world.board().damageTerrainWithProjectile(
                        blockingTileColumn,
                        row,
                        effectiveBaseDamage(),
                        type
                );

                if (hitLimit.isReachedBy(hitCount())) {
                    world.game().unregister(this);
                    return;
                }
            }

            if (isObstacleFirst(obstacle, zombie, previousX)) {
                obstacle.takeProjectileDamage(
                        type,
                        effectiveBaseDamage()
                );
                hitObstacles.add(obstacle);
                if (hitLimit.isReachedBy(hitCount())) {
                    world.game().unregister(this);
                    return;
                }
            }

            if (zombie == null) {
                break;
            }

            type.hitZombie(
                    zombie,
                    effectiveBaseDamage(),
                    tick
            );
            hitZombies.add(zombie);

            if (hitLimit.isReachedBy(hitCount())) {
                world.game().unregister(this);
                return;
            }
        }

        if (hasReachedTerminal()) {
            world.game().unregister(this);
        }
    }
}
