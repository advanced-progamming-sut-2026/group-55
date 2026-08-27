package pvz.model.entity.projectile.homing;

import java.util.Objects;

import pvz.model.entity.zombie.PushedObstacle;

public final class PushedObstacleHomingTarget implements HomingTarget {

    private final PushedObstacle obstacle;

    public PushedObstacleHomingTarget(PushedObstacle obstacle) {
        this.obstacle = Objects.requireNonNull(
                obstacle,
                "target obstacle cannot be null"
        );
    }

    public PushedObstacle obstacle() {
        return obstacle;
    }

    @Override
    public double getX() {
        return obstacle.getX();
    }

    @Override
    public double getY() {
        return obstacle.getY();
    }

    @Override
    public boolean isValid() {
        return !obstacle.isDead();
    }

    @Override
    public void applyImpact(HomingImpact impact, long currentTick) {
        Objects.requireNonNull(impact, "impact cannot be null");

        impact.hitObstacle(obstacle, currentTick);
    }
}
