package pvz.model.core.board;

import java.util.Objects;
import pvz.model.entity.plant.Plant;

public final class TileOverlay {
    private final TileOverlayType type;
    private final Plant coveredPlant;
    private double remainingHealth;

    TileOverlay(TileOverlayType type, Plant coveredPlant) {
        this.type = Objects.requireNonNull(
                type,
                "overlay type cannot be null"
        );
        this.coveredPlant = Objects.requireNonNull(
                coveredPlant,
                "covered plant cannot be null"
        );
        remainingHealth = type.getInitialHealth();
    }

    boolean takeDamage(double damage) {
        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException(
                    "overlay damage must be finite and non-negative"
            );
        }
        if (damage == 0 || isDestroyed()) {
            return false;
        }
        remainingHealth = Math.max(0, remainingHealth - damage);
        return isDestroyed();
    }

    public TileOverlayType getType() {
        return type;
    }

    public Plant getCoveredPlant() {
        return coveredPlant;
    }

    public double getRemainingHealth() {
        return remainingHealth;
    }

    public boolean isDestroyed() {
        return remainingHealth <= 0;
    }
}
