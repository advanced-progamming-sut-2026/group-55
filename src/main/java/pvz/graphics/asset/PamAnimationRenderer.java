package pvz.graphics.asset;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Draws prepared PAM clips fitted inside an arbitrary world-space box. */
public final class PamAnimationRenderer {
    private final PamAnimationService animations;
    private final Map<AnimationKey, Rectangle> boundsCache = new HashMap<>();
    private final Set<AnimationKey> requested = new HashSet<>();
    private final Set<AnimationKey> failed = new HashSet<>();
    private final Color previousColor = new Color();
    private final Matrix4 previousTransform = new Matrix4();
    private final Matrix4 fittedTransform = new Matrix4();

    public PamAnimationRenderer(PamAnimationService animations) {
        this.animations = Objects.requireNonNull(
                animations,
                "animation service cannot be null"
        );
    }

    /**
     * @return true when the PAM frame was drawn; false while loading or when
     * the requested animation is unavailable.
     */
    public boolean draw(
            Batch batch,
            String pamPath,
            String clipName,
            float stateTime,
            float x,
            float y,
            float boxWidth,
            float boxHeight,
            float alpha
    ) {
        return draw(
                batch,
                pamPath,
                clipName,
                clipName,
                stateTime,
                x,
                y,
                boxWidth,
                boxHeight,
                alpha,
                false,
                true,
                Color.WHITE,
                Map.of()
        );
    }

    /** Draws a clip using another clip as its stable fit reference. */
    public boolean draw(
            Batch batch,
            String pamPath,
            String clipName,
            String referenceClipName,
            float stateTime,
            float x,
            float y,
            float boxWidth,
            float boxHeight,
            float alpha
    ) {
        return draw(
                batch,
                pamPath,
                clipName,
                referenceClipName,
                stateTime,
                x,
                y,
                boxWidth,
                boxHeight,
                alpha,
                false,
                true,
                Color.WHITE,
                Map.of()
        );
    }

    /**
     * Draws a fitted PAM clip with optional mirroring, tint and hidden-part
     * overrides. The extended form is used by zombies; the simpler overload
     * remains the plant rendering contract.
     */
    public boolean draw(
            Batch batch,
            String pamPath,
            String clipName,
            float stateTime,
            float x,
            float y,
            float boxWidth,
            float boxHeight,
            float alpha,
            boolean flipX,
            boolean loop,
            Color tint,
            Map<String, Boolean> partsVisibility
    ) {
        return draw(
                batch,
                pamPath,
                clipName,
                clipName,
                stateTime,
                x,
                y,
                boxWidth,
                boxHeight,
                alpha,
                flipX,
                loop,
                tint,
                partsVisibility
        );
    }

    /**
     * Draws {@code clipName} using the stable fitted transform of
     * {@code referenceClipName}. This prevents an entity from changing size
     * or jumping when animation states have different source bounds.
     */
    public boolean draw(
            Batch batch,
            String pamPath,
            String clipName,
            String referenceClipName,
            float stateTime,
            float x,
            float y,
            float boxWidth,
            float boxHeight,
            float alpha,
            boolean flipX,
            boolean loop,
            Color tint,
            Map<String, Boolean> partsVisibility
    ) {
        AnimationKey activeKey = new AnimationKey(pamPath, clipName);
        AnimationKey referenceKey = new AnimationKey(
                pamPath,
                referenceClipName
        );
        Rectangle activeBounds = boundsCache.get(activeKey);
        Rectangle referenceBounds = boundsCache.get(referenceKey);
        if (activeBounds == null) {
            request(activeKey);
        }
        if (referenceBounds == null) {
            request(referenceKey);
        }
        if (activeBounds == null || referenceBounds == null) {
            return false;
        }

        float scale = Math.min(
                boxWidth / referenceBounds.width,
                boxHeight / referenceBounds.height
        );
        float fittedWidth = referenceBounds.width * scale;
        float anchorX = flipX
                ? x + (boxWidth + fittedWidth) / 2f
                        + referenceBounds.x * scale
                : x + (boxWidth - fittedWidth) / 2f
                        - referenceBounds.x * scale;
        // PAM stores vertical bounds in its source (downward-positive)
        // coordinate system. PamPlayer flips that axis while drawing, so the
        // anchor must add the bounds center instead of subtracting bounds.y.
        float anchorY = y + boxHeight / 2f
                + (referenceBounds.y + referenceBounds.height / 2f) * scale;

        previousColor.set(batch.getColor());
        previousTransform.set(batch.getTransformMatrix());
        fittedTransform.set(previousTransform)
                .translate(anchorX, anchorY, 0f)
                .scale(flipX ? -scale : scale, scale, 1f);

        try {
            Color safeTint = tint == null ? Color.WHITE : tint;
            batch.setColor(
                    safeTint.r,
                    safeTint.g,
                    safeTint.b,
                    safeTint.a * alpha
            );
            batch.setTransformMatrix(fittedTransform);
            animations.player().draw(
                    batch,
                    pamPath,
                    clipName,
                    stateTime,
                    0f,
                    0f,
                    loop,
                    partsVisibility == null ? Map.of() : partsVisibility
            );
            return true;
        } catch (RuntimeException exception) {
            boundsCache.remove(activeKey);
            failed.add(activeKey);
            return false;
        } finally {
            batch.setTransformMatrix(previousTransform);
            batch.setColor(previousColor);
        }
    }

    private void request(AnimationKey key) {
        if (failed.contains(key) || !requested.add(key)) {
            return;
        }
        animations.prepare(key.pamPath(), key.clipName(), bounds -> {
            if (bounds == null) {
                failed.add(key);
            } else {
                boundsCache.put(key, new Rectangle(bounds));
            }
        });
    }

    private record AnimationKey(String pamPath, String clipName) {
        private AnimationKey {
            Objects.requireNonNull(pamPath, "PAM path cannot be null");
            Objects.requireNonNull(clipName, "clip name cannot be null");
        }
    }
}
