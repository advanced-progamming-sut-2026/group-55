package pvz.graphics.battle;

/** Pure layout rules shared by the graphical battle health bars. */
public final class HealthBarLayout {
    private HealthBarLayout() {
    }

    public static Bar clampCentered(
            float centerX,
            float desiredBottom,
            float width,
            float height,
            Bounds board
    ) {
        if (!Float.isFinite(centerX)
                || !Float.isFinite(desiredBottom)
                || !Float.isFinite(width)
                || !Float.isFinite(height)
                || width <= 0f
                || height <= 0f
                || board == null) {
            throw new IllegalArgumentException("invalid health-bar layout");
        }

        float fittedWidth = Math.min(width, board.width());
        float fittedHeight = Math.min(height, board.height());
        float left = clamp(
                centerX - fittedWidth / 2f,
                board.left(),
                board.right() - fittedWidth
        );
        float bottom = clamp(
                desiredBottom,
                board.bottom(),
                board.top() - fittedHeight
        );
        return new Bar(left, bottom, fittedWidth, fittedHeight);
    }

    /**
     * Picks the first non-overlapping lane and records the bar's right edge.
     * If every lane is occupied, the lane that frees first is reused.
     */
    public static int reserveLane(
            float left,
            float right,
            float[] occupiedUntil,
            float gap
    ) {
        if (!Float.isFinite(left)
                || !Float.isFinite(right)
                || right < left
                || occupiedUntil == null
                || occupiedUntil.length == 0
                || !Float.isFinite(gap)
                || gap < 0f) {
            throw new IllegalArgumentException("invalid health-bar lane");
        }

        int selected = 0;
        for (int lane = 0; lane < occupiedUntil.length; lane++) {
            if (occupiedUntil[lane] + gap <= left) {
                selected = lane;
                occupiedUntil[lane] = right;
                return selected;
            }
            if (occupiedUntil[lane] < occupiedUntil[selected]) {
                selected = lane;
            }
        }
        occupiedUntil[selected] = right;
        return selected;
    }

    public static float clampRatio(double ratio) {
        if (!Double.isFinite(ratio)) {
            return 0f;
        }
        return (float) Math.max(0d, Math.min(1d, ratio));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Bounds(
            float left,
            float bottom,
            float width,
            float height
    ) {
        public Bounds {
            if (!Float.isFinite(left)
                    || !Float.isFinite(bottom)
                    || !Float.isFinite(width)
                    || !Float.isFinite(height)
                    || width <= 0f
                    || height <= 0f) {
                throw new IllegalArgumentException("invalid board bounds");
            }
        }

        public float right() {
            return left + width;
        }

        public float top() {
            return bottom + height;
        }
    }

    public record Bar(float left, float bottom, float width, float height) {
        public float right() {
            return left + width;
        }
    }
}
