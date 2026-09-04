package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeedPacketStateTest {
    @Test
    void reportsReadyWhenAffordableAndRecharged() {
        SeedPacketState.View view = SeedPacketState.resolve(
                false,
                100,
                150,
                0,
                10
        );

        assertEquals(SeedPacketState.Availability.READY,
                view.availability());
        assertEquals("READY", view.statusText());
        assertTrue(view.selectable());
    }

    @Test
    void reportsCooldownBeforeSunShortage() {
        SeedPacketState.View view = SeedPacketState.resolve(
                false,
                100,
                25,
                23,
                10
        );

        assertEquals(SeedPacketState.Availability.UNAVAILABLE,
                view.availability());
        assertEquals("CD 2.3s", view.statusText());
        assertFalse(view.selectable());
    }

    @Test
    void reportsExactMissingSun() {
        SeedPacketState.View view = SeedPacketState.resolve(
                false,
                125,
                50,
                0,
                10
        );

        assertEquals(SeedPacketState.Availability.UNAVAILABLE,
                view.availability());
        assertEquals("NEED 75 SUN", view.statusText());
    }

    @Test
    void selectedStateTakesVisualPriority() {
        SeedPacketState.View view = SeedPacketState.resolve(
                true,
                100,
                0,
                50,
                10
        );

        assertEquals(SeedPacketState.Availability.SELECTED,
                view.availability());
        assertEquals("SELECTED", view.statusText());
        assertTrue(view.selectable());
    }

    @Test
    void rejectsInvalidPresentationValues() {
        assertThrows(IllegalArgumentException.class,
                () -> SeedPacketState.resolve(false, -1, 0, 0, 10));
        assertThrows(IllegalArgumentException.class,
                () -> SeedPacketState.resolve(false, 1, 0, 0, 0));
    }
}
