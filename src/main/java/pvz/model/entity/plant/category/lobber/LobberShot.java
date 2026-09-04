package pvz.model.entity.plant.category.lobber;

import java.util.Objects;
import pvz.model.entity.projectile.ProjectileType;

public record LobberShot(
        double damage,
        int splashRadius,
        ProjectileType projectileType,
        long butterStunTicks,
        double splashDamageBonus,
        int warmthRadius
) {
    public LobberShot(double damage, int splashRadius, ProjectileType projectileType, long butterStunTicks) {
        this(damage, splashRadius, projectileType, butterStunTicks, 0, 0);
    }

    public LobberShot {
        if (!Double.isFinite(damage) || damage <= 0) {
            throw new IllegalArgumentException("lobber damage must be finite and positive");
        }
        if (splashRadius < 0 || warmthRadius < 0) {
            throw new IllegalArgumentException("lobber radii cannot be negative");
        }
        if (!Double.isFinite(splashDamageBonus) || splashDamageBonus < 0) {
            throw new IllegalArgumentException("splash damage bonus must be finite and non-negative");
        }
        projectileType = Objects.requireNonNull(projectileType, "projectile type cannot be null");
        if (butterStunTicks < 0) {
            throw new IllegalArgumentException("butter stun duration cannot be negative");
        }
    }

    public boolean appliesButterStun() {
        return butterStunTicks > 0;
    }

    public double areaDamage() {
        return damage + splashDamageBonus;
    }
}
