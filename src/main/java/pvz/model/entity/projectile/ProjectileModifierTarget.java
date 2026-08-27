package pvz.model.entity.projectile;

public interface ProjectileModifierTarget {

    ProjectileFamily getProjectileFamily();

    ProjectileType getProjectileType();

    PeaHeatState getPeaHeatState();

    double getPeaDamageMultiplier();

    boolean promotePeaHeat(
            PeaHeatState newState,
            double totalDamageMultiplier
    );
}
