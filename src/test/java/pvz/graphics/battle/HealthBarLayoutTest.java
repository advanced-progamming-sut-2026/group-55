package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HealthBarLayoutTest {
    private static final HealthBarLayout.Bounds BOARD =
            new HealthBarLayout.Bounds(100f, 50f, 700f, 450f);

    @Test
    void clampsBarsInsideEveryBoardEdge() {
        HealthBarLayout.Bar left = HealthBarLayout.clampCentered(
                80f, 20f, 48f, 6f, BOARD
        );
        HealthBarLayout.Bar right = HealthBarLayout.clampCentered(
                830f, 520f, 48f, 6f, BOARD
        );

        assertEquals(100f, left.left());
        assertEquals(50f, left.bottom());
        assertEquals(752f, right.left());
        assertEquals(494f, right.bottom());
    }

    @Test
    void overlappingBarsUseSeparateLanes() {
        float[] lanes = {
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY
        };

        assertEquals(0, HealthBarLayout.reserveLane(100f, 148f, lanes, 3f));
        assertEquals(1, HealthBarLayout.reserveLane(120f, 168f, lanes, 3f));
        assertEquals(0, HealthBarLayout.reserveLane(151f, 199f, lanes, 3f));
    }

    @Test
    void ratiosAreAlwaysSafeForRendering() {
        assertEquals(0f, HealthBarLayout.clampRatio(-1d));
        assertEquals(0.45f, HealthBarLayout.clampRatio(0.45d));
        assertEquals(1f, HealthBarLayout.clampRatio(3d));
        assertEquals(0f, HealthBarLayout.clampRatio(Double.NaN));
    }

    @Test
    void rejectsInvalidGeometry() {
        assertThrows(IllegalArgumentException.class, () ->
                HealthBarLayout.clampCentered(0f, 0f, 0f, 4f, BOARD));
        assertThrows(IllegalArgumentException.class, () ->
                HealthBarLayout.reserveLane(10f, 5f, new float[1], 1f));
    }
}
