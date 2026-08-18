package pvz.model.entity.plant.category.lobber;

import java.util.Objects;

import pvz.model.entity.projectile.ProjectileType;

public record LobberShot(
        double damage,
        int splashRadius,
        ProjectileType projectileType,
        long butterStunTicks
) {
    public LobberShot {
        if (!Double.isFinite(damage) || damage <= 0) {
            throw new IllegalArgumentException(
                    "lobber damage must be finite and non-negative"
            );
        }

        if (splashRadius < 0) {
            throw new IllegalArgumentException(
                    "lobber splash radius cannot be negative"
            );
        }

        projectileType = Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );

        if (butterStunTicks < 0) {
            throw new IllegalArgumentException(
                    "butter stun duration cannot be negative"
            );
        }
    }

    public boolean appliesButterStun() {
        return butterStunTicks > 0;
    }
}
