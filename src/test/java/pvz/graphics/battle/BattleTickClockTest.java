package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BattleTickClockTest {
    @Test
    void convertsFrameTimeToNormalSpeedTicks() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(0, clock.consume(0.05f, 1d, false));
        assertEquals(1, clock.consume(0.05f, 1d, false));
        assertEquals(2, clock.consume(0.20f, 1d, false));
    }

    @Test
    void appliesConfiguredGameSpeedScale() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(2, clock.consume(0.10f, 2d, false));
    }

    @Test
    void appliesDifficultyAndGameSpeedTogether() {
        BattleTickClock clock = new BattleTickClock();
        double scale = BattleTimeScale.effectiveScale(5, 3);

        assertEquals(5, clock.consume(0.10f, scale, false));
    }

    @Test
    void lowerDifficultyCanSlowModelTimeBelowRealtime() {
        BattleTickClock clock = new BattleTickClock();
        double scale = BattleTimeScale.effectiveScale(1, 1);

        assertEquals(0, clock.consume(0.25f, scale, false));
        assertEquals(1, clock.consume(0.051f, scale, false));
    }

    @Test
    void pauseDoesNotAccumulateElapsedTime() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(0, clock.consume(10f, 1d, true));
        assertEquals(1, clock.consume(0.10f, 1d, false));
    }

    @Test
    void resumeResetDropsPartialTimeFromBeforePause() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(0, clock.consume(0.07f, 1d, false));
        assertEquals(0, clock.consume(5f, 1d, true));
        clock.reset();

        assertEquals(0, clock.consume(0.04f, 1d, false));
        assertEquals(1, clock.consume(0.07f, 1d, false));
    }

    @Test
    void stalledFrameUsesBoundedCatchUpAtMaximumScale() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(12, clock.consume(10f, 5d, false));
        assertEquals(1, clock.consume(0.011f, 5d, false));
    }

    @Test
    void speedChangeDoesNotReinterpretOldPartialRealTime() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(
                0,
                clock.consume(
                        0.25f,
                        BattleTimeScale.MIN_EFFECTIVE_SCALE,
                        false
                )
        );
        assertEquals(
                0,
                clock.consume(
                        0.001f,
                        BattleTimeScale.MAX_EFFECTIVE_SCALE,
                        false
                )
        );
        assertEquals(
                1,
                clock.consume(
                        0.003f,
                        BattleTimeScale.MAX_EFFECTIVE_SCALE,
                        false
                )
        );
    }

    @Test
    void rejectsInvalidInputs() {
        BattleTickClock clock = new BattleTickClock();

        assertThrows(
                IllegalArgumentException.class,
                () -> clock.consume(-0.1f, 1d, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> clock.consume(0.1f, 0d, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> clock.consume(0.1f, 6d, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> clock.consume(0.1f, Double.NaN, false)
        );
    }
}
