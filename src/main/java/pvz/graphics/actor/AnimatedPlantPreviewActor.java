package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import java.util.Objects;
import pvz.graphics.asset.PamAnimationService;

/**
 * Compact reusable animated plant preview that falls back to a static texture
 * when a PAM animation is unavailable.
 */
public final class AnimatedPlantPreviewActor extends Group {
    private final PamAnimationService animationService;
    private final String pamPath;
    private final String animationClip;
    private final float previewWidth;
    private final float previewHeight;
    private boolean requested;
    private boolean initialized;

    public AnimatedPlantPreviewActor(
            PamAnimationService animationService,
            String pamPath,
            String animationClip,
            TextureRegion fallback,
            float previewWidth,
            float previewHeight
    ) {
        this.animationService = Objects.requireNonNull(
                animationService,
                "animation service cannot be null"
        );
        this.pamPath = pamPath;
        this.animationClip = animationClip;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        setSize(previewWidth, previewHeight);
        addActor(fallbackPreview(fallback, previewWidth, previewHeight));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (pamPath != null && !requested && !initialized) {
            requested = true;
            animationService.prepare(
                    pamPath,
                    animationClip,
                    this::initializeAnimation
            );
        }
        super.draw(batch, parentAlpha);
    }

    private void initializeAnimation(Rectangle bounds) {
        if (initialized || getStage() == null) {
            return;
        }
        if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
            initialized = true;
            return;
        }

        float usableWidth = previewWidth * 0.84f;
        float usableHeight = previewHeight * 0.84f;
        float scale = Math.min(
                usableWidth / bounds.width,
                usableHeight / bounds.height
        );
        float centerX = previewWidth / 2f;
        float centerY = previewHeight / 2f;

        Group scaler = new Group();
        scaler.setTransform(true);
        scaler.setSize(previewWidth, previewHeight);
        scaler.setOrigin(centerX, centerY);
        scaler.setScale(scale);

        PlantActor actor = new PlantActor(
                animationService.player(),
                pamPath,
                animationClip
        );
        actor.setSize(previewWidth, previewHeight);
        actor.setPosition(
                -bounds.x - bounds.width / 2f,
                centerY - 45f + bounds.y + bounds.height / 2f
        );

        clearChildren();
        scaler.addActor(actor);
        addActor(scaler);
        setColor(getColor());
        initialized = true;
    }
    @Override
    public void setColor(Color color) {
        super.setColor(color);
        for (com.badlogic.gdx.scenes.scene2d.Actor child : getChildren()) {
            child.setColor(color);
        }
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        super.setColor(r, g, b, a);
        for (com.badlogic.gdx.scenes.scene2d.Actor child : getChildren()) {
            child.setColor(r, g, b, a);
        }
    }

    private static Image fallbackPreview(
            TextureRegion fallback,
            float width,
            float height
    ) {
        Image preview = fallback == null ? new Image() : new Image(fallback);
        preview.setScaling(Scaling.fit);
        preview.setSize(width, height);
        return preview;
    }
}
