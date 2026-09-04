package pvz.graphics.battle;

import java.util.Locale;

/** Pure presentation state for a seed packet in the battle HUD. */
public final class SeedPacketState {
    private SeedPacketState() {
    }

    public static View resolve(
            boolean selected,
            int sunCost,
            int availableSun,
            long remainingCooldownTicks,
            int ticksPerSecond
    ) {
        if (sunCost < 0 || availableSun < 0 || remainingCooldownTicks < 0) {
            throw new IllegalArgumentException(
                    "seed packet values cannot be negative"
            );
        }
        if (ticksPerSecond <= 0) {
            throw new IllegalArgumentException(
                    "ticks per second must be positive"
            );
        }
        if (selected) {
            return new View(Availability.SELECTED, "SELECTED");
        }
        if (remainingCooldownTicks > 0) {
            double seconds = remainingCooldownTicks
                    / (double) ticksPerSecond;
            return new View(
                    Availability.UNAVAILABLE,
                    "CD " + String.format(Locale.ROOT, "%.1fs", seconds)
            );
        }
        if (availableSun < sunCost) {
            return new View(
                    Availability.UNAVAILABLE,
                    "NEED " + (sunCost - availableSun) + " SUN"
            );
        }
        return new View(Availability.READY, "READY");
    }

    public enum Availability {
        READY,
        UNAVAILABLE,
        SELECTED
    }

    public record View(Availability availability, String statusText) {
        public boolean selectable() {
            return availability != Availability.UNAVAILABLE;
        }
    }
}
