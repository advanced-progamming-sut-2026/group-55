package pvz.model.entity.projectile;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.Entity;
import pvz.model.entity.plant.lobber.LobberShot;
import pvz.model.entity.plant.lobber.LobberTarget;
import pvz.model.entity.zombie.Zombie;

public final class LobbedProjectile extends Entity {
    private static final double TILES_PER_SECOND = 2;

    private final World world;
    private final LobberTarget target;
    private final LobberShot shot;
    private final long launchTick;
    private final long flightTicks;
    private final double startX;
    private final double startY;

    private double x;
    private double y;
    private boolean impacted;

    public LobbedProjectile(
            World world,
            String name,
            int startColumn,
            int startRow,
            LobberTarget target,
            LobberShot shot,
            long launchTick
    ) {
        this.world = Objects.requireNonNull(
                world,
                "world cannot be null"
        );
        this.name = requireName(name);
        this.target = Objects.requireNonNull(
                target,
                "lobber target cannot be null"
        );
        this.shot = Objects.requireNonNull(
                shot,
                "lobber shot cannot be null"
        );

        if (!world.board().inBounds(startColumn, startRow)) {
            throw new IllegalArgumentException(
                    "lobbed projectile start location is out of bounds"
            );
        }

        if (launchTick < 0) {
            throw new IllegalArgumentException(
                    "launch tick cannot be negative"
            );
        }

        this.launchTick = launchTick;
        this.startX = tileCenter(startColumn);
        this.startY = tileCenter(startRow);
        this.x = startX;
        this.y = startY;
        this.flightTicks = calculateFlightTicks(
                startX,
                tileCenter(clampColumn(target.currentColumn()))
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

    public double getFlightProgress(long currentTick) {
        if (currentTick <= launchTick) {
            return 0;
        }

        return Math.min(
                1,
                (currentTick - launchTick)
                        / (double) flightTicks
        );
    }

    public boolean isButterProjectile() {
        return shot.appliesButterStun();
    }

    @Override
    public void update(long tick) {
        if (impacted) {
            return;
        }

        int impactColumn = clampColumn(target.currentColumn());
        int impactRow = clampRow(target.currentRow());
        double progress = getFlightProgress(tick);

        x = interpolate(
                startX,
                tileCenter(impactColumn),
                progress
        );
        y = interpolate(
                startY,
                tileCenter(impactRow),
                progress
        );

        if (progress < 1) {
            return;
        }

        impact(tick, impactColumn, impactRow);
        impacted = true;
        world.game().unregister(this);
    }

    private void impact(
            long currentTick,
            int impactColumn,
            int impactRow
    ) {
        if (target.targetsZombie()) {
            impactZombieTarget(
                    currentTick,
                    impactColumn,
                    impactRow
            );
            return;
        }

        world.board().damageTerrain(
                impactColumn,
                impactRow,
                shot.projectileType()
                        .damageAgainstTerrain(shot.damage())
        );

        if (shot.splashRadius() > 0) {
            damageArea(
                    currentTick,
                    impactColumn,
                    impactRow
            );
        }
    }

    private void impactZombieTarget(
            long currentTick,
            int impactColumn,
            int impactRow
    ) {
        if (shot.splashRadius() > 0) {
            damageArea(
                    currentTick,
                    impactColumn,
                    impactRow
            );
            return;
        }

        Zombie zombie = target.zombie();

        if (zombie.isDead()) {
            return;
        }

        shot.projectileType().hitZombie(
                zombie,
                shot.damage(),
                currentTick
        );

        applyButterIfNeeded(zombie, currentTick);
    }

    private void damageArea(
            long currentTick,
            int impactColumn,
            int impactRow
    ) {
        world.board().damageZombiesWithProjectileInArea(
                world.getZombies(),
                impactColumn,
                impactRow,
                shot.splashRadius(),
                shot.damage(),
                shot.projectileType(),
                currentTick
        );
    }

    private void applyButterIfNeeded(
            Zombie zombie,
            long currentTick
    ) {
        if (!shot.appliesButterStun()
                || zombie.isDead()) {
            return;
        }

        zombie.applyButterStun(
                currentTick,
                shot.butterStunTicks()
        );
    }

    private long calculateFlightTicks(
            double fromX,
            double toX
    ) {
        double distance = Math.abs(toX - fromX);
        double seconds = distance / TILES_PER_SECOND;

        return Math.max(
                1,
                (long) Math.ceil(
                        seconds * Game.TICKS_PER_SECOND
                )
        );
    }

    private int clampColumn(int column) {
        return Math.max(
                1,
                Math.min(world.board().getCols(), column)
        );
    }

    private int clampRow(int row) {
        return Math.max(
                1,
                Math.min(world.board().getRows(), row)
        );
    }

    private double interpolate(
            double from,
            double to,
            double progress
    ) {
        return from + (to - from) * progress;
    }

    private String requireName(String value) {
        String checkedName = Objects.requireNonNull(
                value,
                "projectile name cannot be null"
        ).strip();

        if (checkedName.isEmpty()) {
            throw new IllegalArgumentException(
                    "projectile name cannot be blank"
            );
        }

        return checkedName;
    }
}
