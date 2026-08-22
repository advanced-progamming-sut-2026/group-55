package pvz.model.entity.zombie;

import java.util.Objects;

public final class ArmorInstance {
    private final ArmorSpec spec;
    private double remainingHealth;

    public ArmorInstance(ArmorSpec spec) {
        this.spec = Objects.requireNonNull(spec, "armor spec cannot be null");
        this.remainingHealth = spec.maxHealth();
    }

    public double absorb(double damage) {
        if (damage <= 0 || isBroken()) {
            return Math.max(0, damage);
        }
        double overflow = Math.max(0, damage - remainingHealth);
        remainingHealth = Math.max(0, remainingHealth - damage);
        return overflow;
    }

    public ArmorSpec spec() {
        return spec;
    }

    public double remainingHealth() {
        return remainingHealth;
    }

    public boolean isBroken() {
        return remainingHealth <= 0;
    }
}
