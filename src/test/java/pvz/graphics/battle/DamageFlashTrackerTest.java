package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DamageFlashTrackerTest {
    @Test
    void firstObservationDoesNotCreateAFlash() {
        DamageFlashTracker<Object> tracker = new DamageFlashTracker<>();
        Object entity = new Object();

        tracker.observe(entity, 100d);

        assertEquals(0f, tracker.intensity(entity));
    }

    @Test
    void durabilityLossStartsAndExpiresTheFlash() {
        DamageFlashTracker<Object> tracker = new DamageFlashTracker<>(0.20f);
        Object entity = new Object();
        tracker.observe(entity, 100d);

        tracker.observe(entity, 80d);
        assertEquals(1f, tracker.intensity(entity));

        tracker.advance(0.10f);
        assertEquals(0.5f, tracker.intensity(entity), 0.0001f);

        tracker.advance(0.10f);
        assertEquals(0f, tracker.intensity(entity));
    }

    @Test
    void healingDoesNotCreateAFlash() {
        DamageFlashTracker<Object> tracker = new DamageFlashTracker<>();
        Object entity = new Object();
        tracker.observe(entity, 80d);

        tracker.observe(entity, 100d);

        assertEquals(0f, tracker.intensity(entity));
    }

    @Test
    void removedKeysLoseTheirSnapshotAndFlash() {
        DamageFlashTracker<Object> tracker = new DamageFlashTracker<>();
        Object entity = new Object();
        tracker.observe(entity, 100d);
        tracker.observe(entity, 70d);

        tracker.retainKeys(Set.of());

        assertEquals(0f, tracker.intensity(entity));
        tracker.observe(entity, 60d);
        assertEquals(0f, tracker.intensity(entity));
    }

    @Test
    void clearDropsEveryTransientSnapshot() {
        DamageFlashTracker<Object> tracker = new DamageFlashTracker<>();
        Object first = new Object();
        Object second = new Object();
        tracker.observe(first, 100d);
        tracker.observe(second, 100d);
        tracker.observe(first, 50d);
        tracker.observe(second, 40d);

        tracker.clear();

        assertEquals(0f, tracker.intensity(first));
        assertEquals(0f, tracker.intensity(second));
        tracker.observe(first, 25d);
        assertEquals(0f, tracker.intensity(first));
    }

    @Test
    void rejectsInvalidTimeAndDurability() {
        DamageFlashTracker<Object> tracker = new DamageFlashTracker<>();
        Object entity = new Object();

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.observe(entity, -1d)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.advance(-0.01f)
        );
        assertTrue(tracker.intensity(entity) == 0f);
    }
}
