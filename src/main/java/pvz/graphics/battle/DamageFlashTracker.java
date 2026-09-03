package pvz.graphics.battle;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Tracks short view-only hit flashes without changing gameplay entities. */
public final class DamageFlashTracker<K> {
    public static final float DEFAULT_DURATION = 0.18f;
    private static final double EPSILON = 0.000_001d;

    private final float duration;
    private final Map<K, Double> durability = new IdentityHashMap<>();
    private final Map<K, Float> remaining = new IdentityHashMap<>();

    public DamageFlashTracker() {
        this(DEFAULT_DURATION);
    }

    public DamageFlashTracker(float duration) {
        if (!Float.isFinite(duration) || duration <= 0f) {
            throw new IllegalArgumentException(
                    "flash duration must be positive and finite"
            );
        }
        this.duration = duration;
    }

    public void observe(K key, double currentDurability) {
        Objects.requireNonNull(key, "flash key cannot be null");
        if (!Double.isFinite(currentDurability) || currentDurability < 0d) {
            throw new IllegalArgumentException(
                    "durability must be finite and non-negative"
            );
        }

        Double previous = durability.put(key, currentDurability);
        if (previous != null
                && currentDurability + EPSILON < previous) {
            remaining.put(key, duration);
        }
    }

    public void advance(float delta) {
        if (!Float.isFinite(delta) || delta < 0f) {
            throw new IllegalArgumentException(
                    "flash delta must be finite and non-negative"
            );
        }
        remaining.replaceAll((key, value) -> Math.max(0f, value - delta));
        remaining.entrySet().removeIf(entry -> entry.getValue() <= 0f);
    }

    public void retainKeys(Set<? extends K> liveKeys) {
        Objects.requireNonNull(liveKeys, "live keys cannot be null");
        durability.keySet().removeIf(key -> !liveKeys.contains(key));
        remaining.keySet().removeIf(key -> !liveKeys.contains(key));
    }

    public void clear() {
        durability.clear();
        remaining.clear();
    }

    public float intensity(K key) {
        Objects.requireNonNull(key, "flash key cannot be null");
        return remaining.getOrDefault(key, 0f) / duration;
    }
}
