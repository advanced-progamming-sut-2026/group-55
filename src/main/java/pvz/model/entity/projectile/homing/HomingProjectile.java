package pvz.model.entity.projectile.homing;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.Entity;

/**
 * Projectile that follows one assigned target until impact. It ignores every
 * other zombie, obstacle or tile on its way, never selects a new target and
 * exposes its live position so the GUI can render it.
 */
public final class HomingProjectile extends Entity {

    public static final double DEFAULT_TILES_PER_SECOND = 2;

    private final World world;

    private final HomingTarget target;

    private final HomingImpact impact;

    private final double tilesPerSecond;

    private double x;

    private double y;

    private boolean finished;

    public HomingProjectile(
            World world,
            String name,
            double startX,
            double startY,
            HomingTarget target,
            HomingImpact impact,
            double tilesPerSecond
    ) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.impact = Objects.requireNonNull(impact, "impact cannot be null");

        if (!Double.isFinite(tilesPerSecond) || tilesPerSecond <= 0) {
            throw new IllegalArgumentException(
                    "projectile speed must be a positive finite value"
            );
        }

        if (!Double.isFinite(startX) || !Double.isFinite(startY)) {
            throw new IllegalArgumentException(
                    "projectile spawn position must be finite"
            );
        }

        this.tilesPerSecond = tilesPerSecond;
        this.x = startX;
        this.y = startY;
    }

    public HomingTarget getTarget() {
        return target;
    }

    public boolean isFinished() {
        return finished;
    }

    public double getStepPerTick() {
        return tilesPerSecond / Game.TICKS_PER_SECOND;
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
        if (finished) {
            return;
        }

        if (!target.isValid()) {
            unregister();
            return;
        }

        double deltaX = target.getX() - x;
        double deltaY = target.getY() - y;
        double distance = Math.hypot(deltaX, deltaY);
        double step = getStepPerTick();

        if (distance <= step) {
            x = target.getX();
            y = target.getY();
            applyImpact(tick);
            return;
        }

        x += deltaX / distance * step;
        y += deltaY / distance * step;
    }

    private void applyImpact(long tick) {
        target.applyImpact(impact, tick);

        GameEvents.publish(
                name + " reached its target at ("
                        + getTileX() + ", " + getTileY() + ")"
        );

        unregister();
    }

    private void unregister() {
        finished = true;
        world.game().unregister(this);
    }
}
