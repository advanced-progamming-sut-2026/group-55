package pvz.model.core.board;

import java.util.Objects;
import pvz.model.entity.projectile.ProjectileEffectProfile;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.Zombie;

public final class ElementInteractionResolver {
    private ElementInteractionResolver() {}

    public static void applyAcceptedZombieHit(
            ProjectileType projectileType,
            Zombie zombie,
            long currentTick
    ) {
        applyAcceptedZombieHit(projectileType, zombie, currentTick, ProjectileEffectProfile.DEFAULT);
    }

    public static void applyAcceptedZombieHit(
            ProjectileType projectileType,
            Zombie zombie,
            long currentTick,
            ProjectileEffectProfile effectProfile
    ) {
        Objects.requireNonNull(projectileType, "projectile type cannot be null");
        Objects.requireNonNull(zombie, "zombie cannot be null");
        Objects.requireNonNull(effectProfile, "projectile effect profile cannot be null");
        if (zombie.isDead()) {
            return;
        }
        switch (projectileType) {
            case NORMAL, ELECTRIC -> { }
            case FIRE -> zombie.clearColdEffects(currentTick);
            case ICE -> zombie.applyChill(currentTick, effectProfile.chillDurationTicks());
            case POISON -> zombie.applyPoison(
                    currentTick,
                    effectProfile.poisonDurationTicks(),
                    effectProfile.poisonDamagePerSecond(),
                    effectProfile.maximumPoisonStacks()
            );
        }
    }

    static boolean damageTile(Tile tile, ProjectileType projectileType, double baseDamage) {
        Objects.requireNonNull(projectileType, "projectile type cannot be null");
        return damageTileWithCalculatedDamage(tile, projectileType, projectileType.calculateDamage(baseDamage));
    }

    static boolean damageTileWithCalculatedDamage(
            Tile tile,
            ProjectileType projectileType,
            double calculatedDamage
    ) {
        Objects.requireNonNull(tile, "tile cannot be null");
        Objects.requireNonNull(projectileType, "projectile type cannot be null");
        if (!Double.isFinite(calculatedDamage) || calculatedDamage < 0) {
            throw new IllegalArgumentException("projectile damage must be finite and non-negative");
        }
        if (projectileType == ProjectileType.FIRE
                && tile.topBlockingOverlayIs(TileOverlayType.FROZEN)) {
            return tile.destroyOverlay(TileOverlayType.FROZEN);
        }
        return tile.takeDamage(calculatedDamage);
    }
}
