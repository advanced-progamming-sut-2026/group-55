package pvz.model.entity.plant.category.homing;

import java.util.Map;
import java.util.Objects;

import pvz.model.entity.projectile.homing.HomingProjectile;

final class HomingProfile {

    private final HomingKind kind;

    private final double damage;

    private final double actionIntervalSeconds;

    private final Map<String, Double> params;

    HomingProfile(
            HomingKind kind,
            double damage,
            double actionIntervalSeconds,
            Map<String, Double> params
    ) {
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");

        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException(
                    "homing damage must be finite and non-negative"
            );
        }

        if (!Double.isFinite(actionIntervalSeconds)
                || actionIntervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "homing action interval must be positive"
            );
        }

        this.damage = damage;
        this.actionIntervalSeconds = actionIntervalSeconds;
        this.params = Map.copyOf(
                Objects.requireNonNull(params, "params cannot be null")
        );
    }

    HomingKind kind() {
        return kind;
    }

    double damage() {
        return damage;
    }

    double actionIntervalSeconds() {
        return actionIntervalSeconds;
    }

    double projectileSpeedTilesPerSecond() {
        return optional(
                "projectileSpeedTilesPerSecond",
                HomingProjectile.DEFAULT_TILES_PER_SECOND
        );
    }

    int plantFoodTargetCount() {
        return (int) required("plantFoodTargetCount");
    }

    int plantFoodProjectileCount() {
        return (int) required("plantFoodProjectileCount");
    }

    long plantFoodProjectileIntervalTicks() {
        return (long) required("plantFoodProjectileIntervalTicks");
    }

    boolean supportsPlantFood() {
        return true;
    }

    private double required(String param) {
        Double value = params.get(param);

        if (value == null) {
            throw new IllegalStateException(
                    "missing homing parameter " + param
                            + " for behavior " + kind
            );
        }

        return value;
    }

    private double optional(String param, double fallback) {
        return params.getOrDefault(param, fallback);
    }
}
