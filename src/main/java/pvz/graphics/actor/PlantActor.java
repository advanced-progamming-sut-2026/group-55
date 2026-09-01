package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.pam.PamPlayer;

public class PlantActor extends Actor {

    private final PamPlayer pamPlayer;
    private final String pamPath;
    private final Color previousBatchColor = new Color();

    private float stateTime;

    public PlantActor(PamPlayer pamPlayer, String pamPath) {
        this.pamPlayer = pamPlayer;
        this.pamPath = pamPath;
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

        pamPlayer.draw(
                batch,
                pamPath,
                "idle",
                stateTime,
                centerX,
                dirtY,
                true
        );

        batch.setColor(previousBatchColor);
    }

    public void resetAnimation() {
        stateTime = 0f;
    }

    public float getStateTime() {
        return stateTime;
    }
}
