package pvz.graphics.asset;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Draws prepared PAM clips fitted inside an arbitrary world-space box. */
public final class PamAnimationRenderer implements Disposable {
    private final PamAnimationService animations;
    private final Map<AnimationKey, Rectangle> boundsCache = new HashMap<>();
    private final Set<AnimationKey> requested = new HashSet<>();
    private final Set<AnimationKey> failed = new HashSet<>();
    private final Map<AnimationKey, PamAnimationService.AnimationRequest> requests =
            new HashMap<>();
    private final Color previousColor = new Color();
    private final Matrix4 previousTransform = new Matrix4();
    private final Matrix4 fittedTransform = new Matrix4();
    private boolean disposed;

    public PamAnimationRenderer(PamAnimationService animations) {
        this.animations = Objects.requireNonNull(
                animations,
                "animation service cannot be null"
        );
    }

    /** Starts preparing a clip without drawing it yet. */
    public void preload(String pamPath, String clipName) {
        if (disposed) {
            return;
        }
        request(new AnimationKey(pamPath, clipName));
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
        if (disposed) {
            return false;
        }
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

        Fit fit = fitReference(
                referenceBounds,
                x,
                y,
                boxWidth,
                boxHeight,
                flipX
        );

        previousColor.set(batch.getColor());
        previousTransform.set(batch.getTransformMatrix());
        fittedTransform.set(previousTransform)
                .translate(fit.anchorX(), fit.anchorY(), 0f)
                .scale(
                        flipX ? -fit.scale() : fit.scale(),
                        fit.scale(),
                        1f
                );

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

    static Fit fitReference(
            Rectangle referenceBounds,
            float x,
            float y,
            float boxWidth,
            float boxHeight,
            boolean flipX
    ) {
        Objects.requireNonNull(
                referenceBounds,
                "reference bounds cannot be null"
        );
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
        // anchor adds the reference bounds center.
        float anchorY = y + boxHeight / 2f
                + (referenceBounds.y + referenceBounds.height / 2f) * scale;
        return new Fit(scale, anchorX, anchorY);
    }

    private void request(AnimationKey key) {
        if (disposed || failed.contains(key) || !requested.add(key)) {
            return;
        }
        PamAnimationService.AnimationRequest request = animations.prepare(
                key.pamPath(),
                key.clipName(),
                bounds -> {
                    requests.remove(key);
                    if (disposed) {
                        return;
                    }
                    if (bounds == null) {
                        failed.add(key);
                    } else {
                        boundsCache.put(key, new Rectangle(bounds));
                    }
                }
        );
        requests.put(key, request);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (PamAnimationService.AnimationRequest request : requests.values()) {
            request.cancel();
        }
        requests.clear();
        boundsCache.clear();
        requested.clear();
        failed.clear();
    }

    private record AnimationKey(String pamPath, String clipName) {
        private AnimationKey {
            Objects.requireNonNull(pamPath, "PAM path cannot be null");
            Objects.requireNonNull(clipName, "clip name cannot be null");
        }
    }

    record Fit(float scale, float anchorX, float anchorY) {
    }
}
