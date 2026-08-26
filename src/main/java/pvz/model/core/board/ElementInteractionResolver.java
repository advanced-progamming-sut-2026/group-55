package pvz.model.core.board;

import java.util.Objects;
import pvz.model.core.Game;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.Zombie;

public final class ElementInteractionResolver {
    private static final long CHILL_DURATION_TICKS =
            10L * Game.TICKS_PER_SECOND;
    private static final long POISON_DURATION_TICKS =
            8L * Game.TICKS_PER_SECOND;
    private static final double POISON_DAMAGE_PER_SECOND = 3;
    private static final int MAXIMUM_POISON_STACKS = 5;

    private ElementInteractionResolver() {}

    public static void applyAcceptedZombieHit(
            ProjectileType projectileType,
            Zombie zombie,
            long currentTick
    ) {
        Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );
        Objects.requireNonNull(zombie, "zombie cannot be null");

        if (zombie.isDead()) {
            return;
        }

        switch (projectileType) {
            case NORMAL -> {
            }
            case FIRE -> zombie.clearColdEffects(currentTick);
            case ICE -> zombie.applyChill(
                    currentTick,
                    CHILL_DURATION_TICKS
            );
            case POISON -> zombie.applyPoison(
                    currentTick,
                    POISON_DURATION_TICKS,
                    POISON_DAMAGE_PER_SECOND,
                    MAXIMUM_POISON_STACKS
            );
        }
    }

    static boolean damageTile(
            Tile tile,
            ProjectileType projectileType,
            double baseDamage
    ) {
        Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );
        return damageTileWithCalculatedDamage(
                tile,
                projectileType,
                projectileType.calculateDamage(baseDamage)
        );
    }

    static boolean damageTileWithCalculatedDamage(
            Tile tile,
            ProjectileType projectileType,
            double calculatedDamage
    ) {
        Objects.requireNonNull(tile, "tile cannot be null");
        Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );

        if (!Double.isFinite(calculatedDamage) || calculatedDamage < 0) {
            throw new IllegalArgumentException(
                    "projectile damage must be finite and non-negative"
            );
        }

        if (projectileType == ProjectileType.FIRE
                && tile.topBlockingOverlayIs(TileOverlayType.FROZEN)) {
            return tile.destroyOverlay(TileOverlayType.FROZEN);
        }

        return tile.takeDamage(calculatedDamage);
    }
}
