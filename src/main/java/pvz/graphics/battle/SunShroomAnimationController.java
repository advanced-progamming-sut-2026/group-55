package pvz.graphics.battle;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Small presentation state machine for Sun-shroom's three real PAM stages.
 *
 * <p>The gameplay model remains the source of truth for the current stage.
 * This class only decides which existing PAM clip should be shown while the
 * model advances from stage 1 to 2 to 3, produces sun, or receives plant
 * food.</p>
 */
public final class SunShroomAnimationController {
    private static final String PLANT_NAME = "sun-shroom";
    private static final String REFERENCE_CLIP = "idle_stage3";
    private static final float GROWTH_DURATION_SECONDS = 1.3333f;
    private static final float STAGE_1_SPECIAL_DURATION_SECONDS = 1.8333f;
    private static final float STAGE_2_SPECIAL_DURATION_SECONDS = 1.8333f;
    private static final float STAGE_3_SPECIAL_DURATION_SECONDS = 1.9333f;

    private static final List<String> CLIPS = List.of(
            // Load the stable full-size reference first to minimize fallback
            // frames when the first Sun-shroom appears on the lawn.
            "idle_stage3",
            "idle_stage1",
            "idle_stage2",
            "growth_stage1",
            "growth_stage2",
            "special_stage1",
            "special_stage2",
            "special_stage3",
            "plantfood_stage1",
            "plantfood_stage2",
            "plantfood_stage3"
    );

    private Phase phase = Phase.IDLE;
    private int displayedStage;
    private int phaseStage;
    private boolean initialized;
    private boolean plantFoodWasActive;
    private long observedActionTick = Long.MIN_VALUE;
    private boolean productionQueued;
    private int queuedProductionStage = 1;

    public static boolean supports(String plantName) {
        return plantName != null
                && PLANT_NAME.equals(plantName.strip().toLowerCase(Locale.ROOT));
    }

    public static List<String> clipsToPreload() {
        return CLIPS;
    }

    public Selection select(
            int modelStage,
            boolean plantFoodActive,
            long actionTick,
            float currentClipTime
    ) {
        validateStage(modelStage);
        initializeIfNeeded(modelStage, actionTick);

        if (plantFoodActive) {
            return selectPlantFood(modelStage, actionTick);
        }
        resetAfterPlantFoodIfNeeded(modelStage, actionTick);
        observeProduction(actionTick, modelStage);

        Selection active = continueOneShot(currentClipTime);
        if (active != null) {
            return active;
        }

        displayedStage = Math.min(displayedStage, modelStage);
        if (modelStage > displayedStage && displayedStage < 3) {
            return startGrowth();
        }
        if (productionQueued) {
            return startProduction();
        }

        displayedStage = modelStage;
        phaseStage = displayedStage;
        return idle(displayedStage);
    }

    private void initializeIfNeeded(int modelStage, long actionTick) {
        if (initialized) {
            return;
        }
        initialized = true;
        displayedStage = modelStage;
        phaseStage = modelStage;
        observedActionTick = actionTick;
    }

    private Selection selectPlantFood(int modelStage, long actionTick) {
        plantFoodWasActive = true;
        phase = Phase.PLANT_FOOD;
        displayedStage = modelStage;
        phaseStage = modelStage;
        productionQueued = false;
        observedActionTick = actionTick;
        return plantFood(modelStage);
    }

    private void resetAfterPlantFoodIfNeeded(
            int modelStage,
            long actionTick
    ) {
        if (!plantFoodWasActive) {
            return;
        }
        plantFoodWasActive = false;
        phase = Phase.IDLE;
        displayedStage = modelStage;
        phaseStage = modelStage;
        productionQueued = false;
        observedActionTick = actionTick;
    }

    private Selection continueOneShot(float currentClipTime) {
        float safeClipTime = Math.max(0f, currentClipTime);
        if (phase == Phase.GROWTH) {
            if (safeClipTime < GROWTH_DURATION_SECONDS) {
                return growth(phaseStage);
            }
            displayedStage = Math.min(3, phaseStage + 1);
            phase = Phase.IDLE;
        } else if (phase == Phase.PRODUCTION) {
            if (safeClipTime < specialDuration(phaseStage)) {
                return production(phaseStage);
            }
            phase = Phase.IDLE;
        }
        return null;
    }

    private Selection startGrowth() {
        phase = Phase.GROWTH;
        phaseStage = displayedStage;
        return growth(phaseStage);
    }

    private Selection startProduction() {
        productionQueued = false;
        phase = Phase.PRODUCTION;
        phaseStage = queuedProductionStage;
        return production(phaseStage);
    }

    private void observeProduction(long actionTick, int modelStage) {
        if (actionTick == Long.MIN_VALUE || actionTick == observedActionTick) {
            return;
        }
        observedActionTick = actionTick;
        productionQueued = true;
        queuedProductionStage = modelStage;
    }

    private static Selection idle(int stage) {
        return new Selection(
                "idle_stage" + stage,
                REFERENCE_CLIP,
                true
        );
    }

    private static Selection growth(int fromStage) {
        if (fromStage < 1 || fromStage > 2) {
            return idle(Math.max(1, Math.min(3, fromStage)));
        }
        return new Selection(
                "growth_stage" + fromStage,
                REFERENCE_CLIP,
                false
        );
    }

    private static Selection production(int stage) {
        return new Selection(
                "special_stage" + stage,
                REFERENCE_CLIP,
                false
        );
    }

    private static Selection plantFood(int stage) {
        return new Selection(
                "plantfood_stage" + stage,
                REFERENCE_CLIP,
                false
        );
    }

    private static float specialDuration(int stage) {
        return switch (stage) {
            case 1 -> STAGE_1_SPECIAL_DURATION_SECONDS;
            case 2 -> STAGE_2_SPECIAL_DURATION_SECONDS;
            case 3 -> STAGE_3_SPECIAL_DURATION_SECONDS;
            default -> throw new IllegalArgumentException(
                    "sun-shroom stage must be between 1 and 3"
            );
        };
    }

    private static void validateStage(int stage) {
        if (stage < 1 || stage > 3) {
            throw new IllegalArgumentException(
                    "sun-shroom stage must be between 1 and 3"
            );
        }
    }

    public record Selection(
            String clip,
            String referenceClip,
            boolean loop
    ) {
        public Selection {
            Objects.requireNonNull(clip, "clip cannot be null");
            Objects.requireNonNull(
                    referenceClip,
                    "reference clip cannot be null"
            );
        }
    }

    private enum Phase {
        IDLE,
        GROWTH,
        PRODUCTION,
        PLANT_FOOD
    }
}
