package pvz.graphics.battle;

import pvz.model.core.Game;

/**
 * Converts render-frame time into deterministic model ticks.
 *
 * <p>The accumulator is intentionally bounded so a stalled window cannot
 * trigger an unbounded catch-up loop when rendering resumes.</p>
 */
public final class BattleTickClock {
    static final float MAX_FRAME_SECONDS = 0.25f;
    static final int MAX_TICKS_PER_FRAME = 6;

    private double accumulatedSeconds;

    public int consume(float deltaSeconds, int gameSpeed, boolean paused) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException(
                    "frame delta must be finite and non-negative"
            );
        }
        if (gameSpeed < 1 || gameSpeed > 3) {
            throw new IllegalArgumentException(
                    "game speed must be between 1 and 3"
            );
        }
        if (paused) {
            return 0;
        }

        accumulatedSeconds += Math.min(deltaSeconds, MAX_FRAME_SECONDS);
        double tickSeconds = 1d / (Game.TICKS_PER_SECOND * gameSpeed);
        int dueTicks = (int) Math.floor(accumulatedSeconds / tickSeconds);

        if (dueTicks <= 0) {
            return 0;
        }
        if (dueTicks > MAX_TICKS_PER_FRAME) {
            accumulatedSeconds = 0d;
            return MAX_TICKS_PER_FRAME;
        }

        accumulatedSeconds -= dueTicks * tickSeconds;
        return dueTicks;
    }

    public void reset() {
        accumulatedSeconds = 0d;
    }
}
