package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BattleTickClockTest {
    @Test
    void convertsFrameTimeToNormalSpeedTicks() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(0, clock.consume(0.05f, 1, false));
        assertEquals(1, clock.consume(0.05f, 1, false));
        assertEquals(2, clock.consume(0.20f, 1, false));
    }

    @Test
    void appliesConfiguredGameSpeed() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(3, clock.consume(0.10f, 3, false));
    }

    @Test
    void pauseDoesNotAccumulateElapsedTime() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(0, clock.consume(10f, 1, true));
        assertEquals(1, clock.consume(0.10f, 1, false));
    }

    @Test
    void resumeResetDropsPartialTimeFromBeforePause() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(0, clock.consume(0.07f, 1, false));
        assertEquals(0, clock.consume(5f, 1, true));
        clock.reset();

        assertEquals(0, clock.consume(0.04f, 1, false));
        assertEquals(1, clock.consume(0.07f, 1, false));
    }

    @Test
    void capsCatchUpWorkAfterAStalledFrame() {
        BattleTickClock clock = new BattleTickClock();

        assertEquals(
                BattleTickClock.MAX_TICKS_PER_FRAME,
                clock.consume(10f, 3, false)
        );
        assertEquals(0, clock.consume(0f, 3, false));
    }

    @Test
    void rejectsInvalidInputs() {
        BattleTickClock clock = new BattleTickClock();

        assertThrows(
                IllegalArgumentException.class,
                () -> clock.consume(-0.1f, 1, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> clock.consume(0.1f, 4, false)
        );
    }
}
