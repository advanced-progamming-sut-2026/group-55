package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import java.util.Objects;
import pvz.graphics.ui.Typography;

/** Shared compact plant identity/cost/status content for loadout and seed cards. */
public final class PlantCompactCardContent extends Table {
    private final Actor previewActor;
    private final Label nameLabel;
    private final Label costLabel;
    private final Label statusLabel;

    public PlantCompactCardContent(
            Skin skin,
            TextureRegion preview,
            String plantName,
            float previewWidth,
            float detailsWidth
    ) {
        this(
                skin,
                createStaticPreview(preview, previewWidth, 64f),
                plantName,
                previewWidth,
                detailsWidth
        );
    }

    public PlantCompactCardContent(
            Skin skin,
            Actor previewActor,
            String plantName,
            float previewWidth,
            float detailsWidth
    ) {
        Objects.requireNonNull(skin, "skin cannot be null");
        this.previewActor = Objects.requireNonNull(
                previewActor,
                "preview actor cannot be null"
        );
        Objects.requireNonNull(plantName, "plant name cannot be null");

        this.previewActor.setSize(previewWidth, 64f);
        add(this.previewActor).size(previewWidth, 64f)
                .padTop(2f)
                .padBottom(2f)
                .padRight(4f);

        Table details = new Table();
        nameLabel = label(skin, plantName, 0.62f);
        nameLabel.setWrap(true);
        details.add(nameLabel).width(detailsWidth).height(28f).row();

        costLabel = label(skin, "", 0.56f);
        details.add(costLabel).width(detailsWidth).height(16f).row();

        statusLabel = label(skin, "", 0.52f);
        details.add(statusLabel).width(detailsWidth).height(16f);
        add(details).size(detailsWidth + 3f, 68f);
    }

    public void update(int sunCost, String status) {
        if (sunCost < 0) {
            throw new IllegalArgumentException("sun cost cannot be negative");
        }
        costLabel.setText(sunCost + " SUN");
        statusLabel.setText(Objects.requireNonNull(status, "status cannot be null"));
    }

    public void setContentColor(Color color) {
        Objects.requireNonNull(color, "content color cannot be null");
        nameLabel.setColor(color);
        costLabel.setColor(color);
        statusLabel.setColor(color);
    }

    public void setPreviewColor(Color color) {
        previewActor.setColor(Objects.requireNonNull(color, "preview color cannot be null"));
    }

    public void setStatusColor(Color color) {
        statusLabel.setColor(Objects.requireNonNull(color, "status color cannot be null"));
    }

    /**
     * Direct scale override for very compact in-battle cards. Unlike the menu
     * readability helper, this deliberately permits smaller text.
     */
    public void setTextScales(
            float nameScale,
            float costScale,
            float statusScale
    ) {
        if (nameScale <= 0f || costScale <= 0f || statusScale <= 0f) {
            throw new IllegalArgumentException("text scales must be positive");
        }
        nameLabel.setFontScale(nameScale);
        costLabel.setFontScale(costScale);
        statusLabel.setFontScale(statusScale);
    }

    private static Actor createStaticPreview(
            TextureRegion preview,
            float previewWidth,
            float previewHeight
    ) {
        Image image = preview == null ? new Image() : new Image(preview);
        image.setScaling(Scaling.fit);
        image.setSize(previewWidth, previewHeight);
        return image;
    }

    private static Label label(Skin skin, String text, float scale) {
        Label label = new Label(text, skin);
        Typography.applyBody(label, skin);
        Typography.setReadableScale(label, scale);
        label.setColor(Color.DARK_GRAY);
        label.setAlignment(Align.center);
        return label;
    }
}
