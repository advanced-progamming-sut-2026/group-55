package pvz.graphics.asset;

import com.badlogic.gdx.graphics.Color;
import java.util.List;
import java.util.Objects;
import pvz.model.core.Game;
import pvz.model.entity.collectible.sun.Sun;

/** Central presentation metadata for battle suns and radioactive explosions. */
public final class SunVisualResolver {
    private static final String SUN_PAM =
            "768/INITIAL/EFFECTS/SUN/SUN.PAM";
    private static final String SUN_CLIP = "animation";
    private static final String RADIOACTIVE_SUN_CLIP = "blue";
    private static final String EXPLOSION_PAM =
            "768/INITIAL/EFFECTS/TELEPORTATO_EXPLOSION/"
                    + "TELEPORTATO_EXPLOSION.PAM";
    private static final List<String> EXPLOSION_CLIPS = List.of(
            "animation",
            "animation2",
            "animation3"
    );

    // The 50-sun is the visual baseline.  It is intentionally twice the
    // size used by the first graphical Sun pass (0.48 cell).  The canonical
    // 25/50/75/100 values scale from this baseline at exact 0.5/1/1.5/2
    // ratios so their value is immediately readable on the lawn.
    private static final float NORMAL_SUN_SIZE_IN_CELL = 0.96f;
    private static final float HITBOX_SCALE = 0.86f;
    private static final float PLANT_POP_DURATION_SECONDS = 0.45f;
    private static final float PLANT_POP_HEIGHT_IN_CELL = 0.18f;
    private static final float EXPLOSION_DURATION_SECONDS = 1.1667f;
    private static final float EXPLOSION_SIZE_IN_CELLS = 2.80f;

    private static final Color NORMAL_TINT = Color.WHITE;
    private static final Color RADIOACTIVE_TINT =
            new Color(0.90f, 0.76f, 1.00f, 1.00f);
    private static final Color RADIOACTIVE_EXPLOSION_TINT =
            new Color(0.96f, 0.72f, 1.00f, 1.00f);

    public Visual resolve(Sun sun, long tick) {
        Objects.requireNonNull(sun, "sun cannot be null");

        boolean radioactive = sun.isRadioactiveWhileFalling();
        float size = radioactive
                ? NORMAL_SUN_SIZE_IN_CELL
                : NORMAL_SUN_SIZE_IN_CELL * sizeScaleForValue(sun.getValue());

        return new Visual(
                SUN_PAM,
                radioactive ? RADIOACTIVE_SUN_CLIP : SUN_CLIP,
                size,
                radioactive ? RADIOACTIVE_TINT : NORMAL_TINT
        );
    }

    public float hitboxScale() {
        return HITBOX_SCALE;
    }

    public float plantPopOffsetInCells(Sun sun, long tick) {
        Objects.requireNonNull(sun, "sun cannot be null");
        double ageTicks = Math.max(0L, tick - sun.getSpawnTick());
        double durationTicks = PLANT_POP_DURATION_SECONDS
                * Game.TICKS_PER_SECOND;
        if (durationTicks <= 0d || ageTicks >= durationTicks) {
            return 0f;
        }

        double progress = ageTicks / durationTicks;
        return (float) Math.sin(Math.PI * progress)
                * PLANT_POP_HEIGHT_IN_CELL;
    }

    public String explosionPamPath() {
        return EXPLOSION_PAM;
    }

    public List<String> explosionClips() {
        return EXPLOSION_CLIPS;
    }

    public String explosionClip(int sequence) {
        return EXPLOSION_CLIPS.get(
                Math.floorMod(sequence, EXPLOSION_CLIPS.size())
        );
    }

    public float explosionDurationSeconds() {
        return EXPLOSION_DURATION_SECONDS;
    }

    public float explosionSizeInCells() {
        return EXPLOSION_SIZE_IN_CELLS;
    }

    public Color explosionTint() {
        return RADIOACTIVE_EXPLOSION_TINT;
    }

    private static float sizeScaleForValue(int value) {
        if (value <= 25) {
            return 0.50f;
        }
        if (value <= 50) {
            return 1.00f;
        }
        if (value <= 75) {
            return 1.50f;
        }
        // 100-sun is exactly twice the 50-sun.  Higher-value producer
        // rewards keep this maximum battle footprint instead of growing
        // beyond two cells and covering a large part of the board.
        return 2.00f;
    }

    public record Visual(
            String pamPath,
            String clipName,
            float sizeInCell,
            Color tint
    ) {
        public Visual {
            Objects.requireNonNull(pamPath, "PAM path cannot be null");
            Objects.requireNonNull(clipName, "clip name cannot be null");
            Objects.requireNonNull(tint, "tint cannot be null");
            if (sizeInCell <= 0f) {
                throw new IllegalArgumentException(
                        "sun visual size must be positive"
                );
            }
        }
    }
}
