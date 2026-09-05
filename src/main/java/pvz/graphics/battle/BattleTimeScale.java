package pvz.graphics.battle;

import pvz.model.service.DifficultyService;

/**
 * Resolves the real-time speed of an active battle.
 *
 * <p>The model keeps {@code Game.TICKS_PER_SECOND} fixed. Difficulty changes
 * how quickly those model ticks are delivered in real time, while the user's
 * game-speed setting acts as a separate fast-forward multiplier.</p>
 */
public final class BattleTimeScale {
    public static final int MIN_DIFFICULTY_LEVEL = 1;
    public static final int MAX_DIFFICULTY_LEVEL = 5;
    public static final int MIN_GAME_SPEED = 1;
    public static final int MAX_GAME_SPEED = 3;
    public static final float MAX_FRAME_SECONDS = 0.25f;

    public static final double MIN_EFFECTIVE_SCALE = 1d / 3d;
    public static final double MAX_EFFECTIVE_SCALE = 5d;

    private BattleTimeScale() {
    }

    public static double difficultyScale(int difficultyLevel) {
        validateDifficulty(difficultyLevel);
        return DifficultyService.getIncreaseMultiplier(difficultyLevel);
    }

    public static double effectiveScale(
            int difficultyLevel,
            int gameSpeed
    ) {
        validateGameSpeed(gameSpeed);
        return difficultyScale(difficultyLevel) * gameSpeed;
    }

    private static void validateDifficulty(int difficultyLevel) {
        if (difficultyLevel < MIN_DIFFICULTY_LEVEL
                || difficultyLevel > MAX_DIFFICULTY_LEVEL) {
            throw new IllegalArgumentException(
                    "difficulty level must be between 1 and 5"
            );
        }
    }

    private static void validateGameSpeed(int gameSpeed) {
        if (gameSpeed < MIN_GAME_SPEED || gameSpeed > MAX_GAME_SPEED) {
            throw new IllegalArgumentException(
                    "game speed must be between 1 and 3"
            );
        }
    }
}
