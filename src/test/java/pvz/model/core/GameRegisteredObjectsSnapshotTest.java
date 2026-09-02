package pvz.model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GameRegisteredObjectsSnapshotTest {

    @Test
    void returnsImmutableTypeFilteredSnapshot() {
        Game game = new Game();
        TestUpdatable first = new TestUpdatable();
        TestUpdatable second = new TestUpdatable();
        OtherUpdatable other = new OtherUpdatable();
        game.register(first);
        game.register(other);
        game.register(second);

        List<TestUpdatable> snapshot = game.getRegisteredObjects(
                TestUpdatable.class
        );

        assertEquals(List.of(first, second), snapshot);
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(new TestUpdatable())
        );
    }

    @Test
    void reflectsPendingChangesWithoutExposingRemovedObject() {
        Game game = new Game();
        TestUpdatable addedDuringTick = new TestUpdatable();
        SnapshotObserver observer = new SnapshotObserver(
                game,
                addedDuringTick
        );
        game.register(observer);

        game.advance(1);

        assertFalse(observer.snapshotDuringUpdate().contains(observer));
        assertTrue(observer.snapshotDuringUpdate().contains(addedDuringTick));
        assertEquals(
                List.of(addedDuringTick),
                game.getRegisteredObjects(TestUpdatable.class)
        );
    }

    private static class TestUpdatable implements Updatable {
        @Override
        public void update(long tick) {
        }
    }

    private static final class OtherUpdatable implements Updatable {
        @Override
        public void update(long tick) {
        }
    }

    private static final class SnapshotObserver extends TestUpdatable {
        private final Game game;
        private final TestUpdatable addition;
        private List<TestUpdatable> snapshotDuringUpdate = List.of();

        private SnapshotObserver(Game game, TestUpdatable addition) {
            this.game = game;
            this.addition = addition;
        }

        @Override
        public void update(long tick) {
            game.unregister(this);
            game.register(addition);
            snapshotDuringUpdate = game.getRegisteredObjects(
                    TestUpdatable.class
            );
        }

        private List<TestUpdatable> snapshotDuringUpdate() {
            return snapshotDuringUpdate;
        }
    }
}
