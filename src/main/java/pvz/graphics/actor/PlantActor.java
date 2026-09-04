package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.pam.PamPlayer;

public class PlantActor extends Actor {

    private final PamPlayer pamPlayer;
    private final String pamPath;
    private final Color previousBatchColor = new Color();

    private String clipName;
    private float stateTime;
    private String cachedClipName = null;

    public PlantActor(PamPlayer pamPlayer, String pamPath) {
        this(pamPlayer, pamPath, "idle");
    }

    public PlantActor(
            PamPlayer pamPlayer,
            String pamPath,
            String clipName
    ) {
        this.pamPlayer = pamPlayer;
        this.pamPath = pamPath;
        this.clipName = clipName;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (pamPlayer == null || pamPath == null || pamPath.isBlank()) {
            return;
        }

        float centerX = getX() + getWidth() / 2f;
        float dirtY = getY() + 45f;
        Color color = getColor();

        previousBatchColor.set(batch.getColor());
        batch.setColor(
                color.r,
                color.g,
                color.b,
                color.a * parentAlpha
        );

        try {
            if (cachedClipName != null) {
                try {
                    pamPlayer.draw(batch, pamPath, cachedClipName, stateTime, centerX, dirtY, true);
                    return;
                } catch (Exception e) {
                    cachedClipName = null;
                }
            }

            String[] candidates = {
                    "idle",
                    "idle_stage1",
                    "idle1"
            };

            for (String candidate : candidates) {
                try {
                    pamPlayer.draw(batch, pamPath, candidate, stateTime, centerX, dirtY, true);
                    cachedClipName = candidate;
                    return;
                } catch (IllegalArgumentException ignored) {
                }
            }
        } finally {
            batch.setColor(previousBatchColor);
        }
    }

    public void resetAnimation() {
        stateTime = 0f;
    }

    public float getStateTime() {
        return stateTime;
    }
}
