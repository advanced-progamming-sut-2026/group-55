package pvz.model.entity.plant.category.explosive;

import java.util.Map;
import java.util.Objects;

public final class ExplosiveProfile {

    private static final long DEFAULT_EFFECT_DISPLAY_TICKS = 5;

    private final ExplosiveKind kind;

    private final double damage;

    private final Map<String, Double> params;

    ExplosiveProfile(
            ExplosiveKind kind,
            double damage,
            Map<String, Double> params
    ) {
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");

        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException(
                    "explosive damage must be finite and non-negative"
            );
        }

        this.damage = damage;
        this.params = Map.copyOf(
                Objects.requireNonNull(params, "params cannot be null")
        );
    }

    public ExplosiveKind kind() {
        return kind;
    }

    public double damage() {
        return damage;
    }

    public int explosionRadius() {
        return (int) optional("explosionRadius", 0);
    }

    public long armDelayTicks() {
        return (long) required("armDelayTicks");
    }

    public int plantFoodCloneCount() {
        return (int) required("plantFoodCloneCount");
    }

    public int plantFoodTargetCount() {
        return (int) required("plantFoodTargetCount");
    }

    public int scanDistanceTiles() {
        return (int) required("scanDistanceTiles");
    }

    public double grapeDamage() {
        return required("grapeDamage");
    }

    public int maxBounces() {
        return (int) required("maxBounces");
    }

    public long maxLifetimeTicks() {
        return (long) required("maxLifetimeTicks");
    }

    public long stepIntervalTicks() {
        return (long) optional("stepIntervalTicks", 1);
    }

    public long freezeDurationTicks() {
        return (long) required("freezeDurationTicks");
    }

    public long craterDurationTicks() {
        return (long) required("craterDurationTicks");
    }

    public long effectDisplayTicks() {
        return (long) optional(
                "effectDisplayTicks",
                DEFAULT_EFFECT_DISPLAY_TICKS
        );
    }

    public boolean supportsPlantFood() {
        return switch (kind) {
            case MINE, SQUASH, TANGLE_KELP, FREEZE_TRAP -> true;
            default -> false;
        };
    }

    private double required(String param) {
        Double value = params.get(param);

        if (value == null) {
            throw new IllegalStateException(
                    "missing explosive parameter " + param
                            + " for behavior " + kind
            );
        }

        return value;
    }

    private double optional(String param, double fallback) {
        return params.getOrDefault(param, fallback);
    }
}
