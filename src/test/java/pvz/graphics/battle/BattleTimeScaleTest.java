package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BattleTimeScaleTest {
    private static final double EPSILON = 1e-9;

    @Test
    void difficultyThreeKeepsTheReferenceModelSpeed() {
        assertEquals(1d, BattleTimeScale.difficultyScale(3), EPSILON);
        assertEquals(1d, BattleTimeScale.effectiveScale(3, 1), EPSILON);
    }

    @Test
    void difficultyUsesTheDocumentedDifficultyOverThreeFactor() {
        assertEquals(
                1d / 3d,
                BattleTimeScale.difficultyScale(1),
                EPSILON
        );
        assertEquals(
                5d / 3d,
                BattleTimeScale.difficultyScale(5),
                EPSILON
        );
    }

    @Test
    void userGameSpeedMultipliesTheDifficultyScale() {
        assertEquals(2d, BattleTimeScale.effectiveScale(3, 2), EPSILON);
        assertEquals(5d, BattleTimeScale.effectiveScale(5, 3), EPSILON);
        assertEquals(2d / 3d, BattleTimeScale.effectiveScale(2, 1), EPSILON);
    }

    @Test
    void rejectsOutOfRangeSettings() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BattleTimeScale.effectiveScale(0, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> BattleTimeScale.effectiveScale(6, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> BattleTimeScale.effectiveScale(3, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> BattleTimeScale.effectiveScale(3, 4)
        );
    }
}
