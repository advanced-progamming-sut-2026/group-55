package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import pvz.model.core.Game;
import pvz.model.core.Updatable;

class BattleTimeScaleGameIntegrationTest {
    @Test
    void oneClockDecisionAdvancesEveryRegisteredUpdatableTogether() {
        Game game = new Game();
        CountingUpdatable first = new CountingUpdatable();
        CountingUpdatable second = new CountingUpdatable();
        game.register(first);
        game.register(second);

        BattleTickClock clock = new BattleTickClock();
        int ticks = clock.consume(
                0.10f,
                BattleTimeScale.effectiveScale(3, 2),
                false
        );
        game.advance(ticks);

        assertEquals(2, ticks);
        assertEquals(2, game.getCurrentTick());
        assertEquals(2, first.updateCount);
        assertEquals(2, second.updateCount);
    }

    private static final class CountingUpdatable implements Updatable {
        private int updateCount;

        @Override
        public void update(long tick) {
            updateCount++;
        }
    }
}
