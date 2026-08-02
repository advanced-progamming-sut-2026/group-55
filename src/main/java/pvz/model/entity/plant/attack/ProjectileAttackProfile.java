package pvz.model.entity.plant.attack;

import java.util.List;

import pvz.model.entity.projectile.ProjectileType;

public interface ProjectileAttackProfile {

    double damagePerProjectile();

    long ticksBetweenShots();

    List<ShotPath> shotPaths();

    ProjectileType projectileType();

    int rangeTiles();

    default int burstLength() {
        return shotPaths().stream()
                .mapToInt(ShotPath::shotsPerVolley)
                .max()
                .orElseThrow();
    }
}
