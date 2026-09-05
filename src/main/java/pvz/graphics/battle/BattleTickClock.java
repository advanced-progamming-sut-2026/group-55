package pvz.graphics.battle;

import pvz.model.core.Game;

/**
 * Converts render-frame time into deterministic model ticks.
 *
 * <p>{@link Game#TICKS_PER_SECOND} remains the fixed definition of one model
 * second. The supplied time scale only changes how many of those ticks are
 * delivered per real second. This makes every registered model updatable speed
 * up or slow down together without changing its own timing constants.</p>
 *
 * <p>The accumulator stores fractional model ticks instead of real seconds.
 * This is important if the user-facing speed changes: time already accumulated
 * at the old speed is not reinterpreted using the new multiplier.</p>
 *
 * <p>Frame time is bounded so a stalled window cannot trigger an unbounded
 * catch-up loop when rendering resumes.</p>
 */
public final class BattleTickClock {
    static final float MAX_FRAME_SECONDS = BattleTimeScale.MAX_FRAME_SECONDS;
    static final int MAX_TICKS_PER_FRAME = 13;

    private double accumulatedTicks;

    public int consume(
            float deltaSeconds,
            double timeScale,
            boolean paused
    ) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0f) {
            throw new IllegalArgumentException(
                    "frame delta must be finite and non-negative"
            );
        }
        if (!Double.isFinite(timeScale)
                || timeScale < BattleTimeScale.MIN_EFFECTIVE_SCALE
                || timeScale > BattleTimeScale.MAX_EFFECTIVE_SCALE) {
            throw new IllegalArgumentException(
                    "time scale must be between 1/3 and 5"
            );
        }
        if (paused) {
            return 0;
        }

        double frameSeconds = Math.min(deltaSeconds, MAX_FRAME_SECONDS);
        accumulatedTicks += frameSeconds
                * Game.TICKS_PER_SECOND
                * timeScale;

        int dueTicks = (int) Math.floor(accumulatedTicks);
        if (dueTicks <= 0) {
            return 0;
        }
        if (dueTicks > MAX_TICKS_PER_FRAME) {
            accumulatedTicks = 0d;
            return MAX_TICKS_PER_FRAME;
        }

        accumulatedTicks -= dueTicks;
        return dueTicks;
    }

    public void reset() {
        accumulatedTicks = 0d;
    }
}
