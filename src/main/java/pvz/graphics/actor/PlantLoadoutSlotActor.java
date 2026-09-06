package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import java.util.Objects;
import pvz.graphics.asset.PamAnimationService;
import pvz.graphics.ui.HoverEffect;
import pvz.graphics.ui.Typography;

/** Compact plant card for loadout/seed-bank style presentations. */
public final class PlantLoadoutSlotActor extends Table {
    public static final float WIDTH = 224f;
    public static final float HEIGHT = 80f;

    public PlantLoadoutSlotActor(
            Skin skin,
            PamAnimationService animationService,
            TextureRegion preview,
            String pamPath,
            String animationClip,
            String name,
            int sunCost,
            String status,
            boolean removable,
            Runnable removeAction
    ) {
        Objects.requireNonNull(skin, "skin cannot be null");
        Objects.requireNonNull(name, "plant name cannot be null");
        Objects.requireNonNull(status, "plant status cannot be null");

        setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        setSize(WIDTH, HEIGHT);
        pad(4f);

        Actor previewActor = pamPath == null
                ? null
                : new AnimatedPlantPreviewActor(
                        animationService,
                        pamPath,
                        animationClip,
                        preview,
                        54f,
                        64f
                );
        PlantCompactCardContent content = previewActor == null
                ? new PlantCompactCardContent(
                        skin,
                        preview,
                        name,
                        54f,
                        140f
                )
                : new PlantCompactCardContent(
                        skin,
                        previewActor,
                        name,
                        54f,
                        140f
                );
        content.setTextScales(0.50f, 0.45f, 0.41f);
        content.update(sunCost, status);
        content.setStatusColor(status.contains("BOOST") ? Color.FOREST : Color.DARK_GRAY);
        add(content).grow();

        if (removable) {
            Objects.requireNonNull(removeAction, "remove action cannot be null");
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    event.stop();
                    removeAction.run();
                }
            });
            HoverEffect.addScale(this);
        }
    }

    public static Table empty(Skin skin, int slotNumber, boolean locked) {
        Table slot = new Table();
        slot.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        slot.setColor(locked ? Color.GRAY : Color.WHITE);
        Label label = new Label(
                locked ? "SLOT " + slotNumber + " - LOCKED" : "SLOT " + slotNumber + " - EMPTY",
                skin
        );
        Typography.applyBody(label, skin);
        Typography.setReadableScale(label, 0.62f);
        label.setColor(locked ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        label.setAlignment(Align.center);
        slot.add(label).width(WIDTH - 12f).height(HEIGHT - 8f);
        return slot;
    }
}
