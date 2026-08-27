package pvz.model.entity.projectile.homing;

import pvz.model.core.World;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

/**
 * Effect that a homing projectile applies when it reaches its target. The
 * projectile itself never knows which plant fired it.
 */
public interface HomingImpact {

    void hitZombie(Zombie zombie, long currentTick);

    default void hitTile(
            World world,
            int column,
            int row,
            long currentTick
    ) {
    }

    default void hitObstacle(
            PushedObstacle obstacle,
            long currentTick
    ) {
    }
}
