package pvz.graphics.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

class PamAnimationRendererFitTest {
    @Test
    void referenceBoundsDetermineTheStableScaleAndAnchor() {
        Rectangle idleBounds = new Rectangle(10f, 20f, 100f, 200f);

        PamAnimationRenderer.Fit fit = PamAnimationRenderer.fitReference(
                idleBounds,
                5f,
                7f,
                90f,
                96f,
                false
        );

        assertEquals(0.48f, fit.scale(), 0.0001f);
        assertEquals(21.2f, fit.anchorX(), 0.0001f);
        assertEquals(112.6f, fit.anchorY(), 0.0001f);
    }

    @Test
    void mirroringChangesOnlyTheHorizontalAnchor() {
        Rectangle walkBounds = new Rectangle(-15f, 5f, 80f, 120f);

        PamAnimationRenderer.Fit normal = PamAnimationRenderer.fitReference(
                walkBounds, 10f, 20f, 80f, 120f, false
        );
        PamAnimationRenderer.Fit mirrored =
                PamAnimationRenderer.fitReference(
                        walkBounds, 10f, 20f, 80f, 120f, true
                );

        assertEquals(normal.scale(), mirrored.scale(), 0.0001f);
        assertEquals(normal.anchorY(), mirrored.anchorY(), 0.0001f);
        assertEquals(75f, mirrored.anchorX(), 0.0001f);
    }
}
