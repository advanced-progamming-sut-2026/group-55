package pvz.model.entity.plant.category.homing;

import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.projectile.homing.HomingImpact;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

final class ElectricBlueberryImpact implements HomingImpact {

    private final double damage;

    ElectricBlueberryImpact(double damage) {
        if (!Double.isFinite(damage) || damage <= 0) {
            throw new IllegalArgumentException(
                    "electric blueberry damage must be positive"
            );
        }

        this.damage = damage;
    }

    @Override
    public void hitZombie(Zombie zombie, long currentTick) {
        Objects.requireNonNull(zombie, "zombie cannot be null");

        zombie.takeAbilityDamage(
                damage,
                DamageContext.AttackDelivery.HOMING,
                DamageContext.ImpactMode.SINGLE_TARGET
        );
    }

    @Override
    public void hitTile(
            World world,
            int column,
            int row,
            long currentTick
    ) {
        world.board().damageTerrain(column, row, damage);
    }

    @Override
    public void hitObstacle(PushedObstacle obstacle, long currentTick) {
        obstacle.takeDirectDamage(damage);
    }
}
