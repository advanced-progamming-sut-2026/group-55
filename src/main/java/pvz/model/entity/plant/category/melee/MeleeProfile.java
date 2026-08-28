package pvz.model.entity.plant.category.melee;

import java.util.Map;
import java.util.Objects;

final class MeleeProfile {

    private final MeleeKind kind;
    private final double damage;
    private final double actionIntervalSeconds;
    private final Map<String, Double> params;

    MeleeProfile(
            MeleeKind kind,
            double damage,
            double actionIntervalSeconds,
            Map<String, Double> params
    ) {
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException(
                    "melee damage must be finite and non-negative"
            );
        }
        if (!Double.isFinite(actionIntervalSeconds)
                || actionIntervalSeconds < 0) {
            throw new IllegalArgumentException(
                    "melee action interval must be finite and non-negative"
            );
        }
        this.damage = damage;
        this.actionIntervalSeconds = actionIntervalSeconds;
        this.params = Map.copyOf(
                Objects.requireNonNull(params, "params cannot be null")
        );
    }

    MeleeKind kind() {
        return kind;
    }

    double damage() {
        return damage;
    }

    double actionIntervalSeconds() {
        return actionIntervalSeconds;
    }

    int rangeTiles() {
        return (int) optional("rangeTiles", 1);
    }

    int plantFoodRadius() {
        return (int) optional("plantFoodRadius", 1);
    }

    int plantFoodHitCount() {
        return (int) optional("plantFoodHitCount", 1);
    }

    double plantFoodDamageMultiplier() {
        return optional("plantFoodDamageMultiplier", 1);
    }

    long digestTicks() {
        return (long) required("digestTicks");
    }

    int plantFoodTargetCount() {
        return (int) required("plantFoodTargetCount");
    }

    int plantFoodRangeTiles() {
        return (int) required("plantFoodRangeTiles");
    }

    long stageTwoTicks() {
        return (long) required("stageTwoTicks");
    }

    long stageThreeTicks() {
        return (long) required("stageThreeTicks");
    }

    double stageTwoDamage() {
        return required("stageTwoDamage");
    }

    double stageThreeDamage() {
        return required("stageThreeDamage");
    }


    long stageFourTicks() {
        return (long) optional("stageFourTicks", stageThreeTicks());
    }

    double stageFourDamage() {
        return optional("stageFourDamage", stageThreeDamage());
    }

    int maxGrowthStage() {
        return (int) optional("maxGrowthStage", 3);
    }

    int plantFoodGrowthStage() {
        return (int) required("plantFoodGrowthStage");
    }

    boolean supportsPlantFood() {
        return true;
    }

    private double required(String param) {
        Double value = params.get(param);
        if (value == null) {
            throw new IllegalStateException(
                    "missing melee parameter " + param
                            + " for behavior " + kind
            );
        }
        return value;
    }

    private double optional(String param, double fallback) {
        return params.getOrDefault(param, fallback);
    }
}
