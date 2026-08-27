package pvz.model.entity.projectile.homing;

/**
 * Something a homing projectile can follow. The projectile reads the live
 * position every tick instead of caching the spawn coordinates.
 */
public interface HomingTarget {

    double getX();

    double getY();

    boolean isValid();

    void applyImpact(HomingImpact impact, long currentTick);
}
