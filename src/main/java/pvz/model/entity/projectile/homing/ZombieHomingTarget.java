package pvz.model.entity.projectile.homing;

import java.util.Objects;

import pvz.model.entity.zombie.Zombie;

public final class ZombieHomingTarget implements HomingTarget {

    private final Zombie zombie;

    public ZombieHomingTarget(Zombie zombie) {
        this.zombie = Objects.requireNonNull(
                zombie,
                "target zombie cannot be null"
        );
    }

    public Zombie zombie() {
        return zombie;
    }

    @Override
    public double getX() {
        return zombie.getX();
    }

    @Override
    public double getY() {
        return zombie.getY();
    }

    @Override
    public boolean isValid() {
        return !zombie.isDead()
                && zombie.getWorld() != null
                && zombie.isHostile();
    }

    @Override
    public void applyImpact(HomingImpact impact, long currentTick) {
        Objects.requireNonNull(impact, "impact cannot be null");

        impact.hitZombie(zombie, currentTick);
    }
}
