package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import java.util.Objects;
import pvz.graphics.battle.SeedPacketState;

/** Reusable graphical seed packet used by the in-battle seed bank. */
public final class BattleSeedPacketActor extends Table implements Disposable {
    public static final float PACKET_WIDTH = 138f;
    public static final float PACKET_HEIGHT = 72f;

    private static final Color READY_COLOR = new Color(1f, 1f, 1f, 1f);
    private static final Color UNAVAILABLE_COLOR =
            new Color(0.48f, 0.48f, 0.48f, 1f);
    private static final Color SELECTED_COLOR =
            new Color(1f, 0.80f, 0.18f, 1f);
    private static final Color READY_TEXT_COLOR =
            new Color(0.10f, 0.52f, 0.10f, 1f);

    private final Image previewImage;
    private final Label nameLabel;
    private final Label costLabel;
    private final Label statusLabel;
    private Runnable selectionAction;
    private final ClickListener clickListener;
    private boolean disposed;

    private SeedPacketState.View state = new SeedPacketState.View(
            SeedPacketState.Availability.UNAVAILABLE,
            "UNAVAILABLE"
    );

    public BattleSeedPacketActor(
            Skin skin,
            String plantName,
            TextureRegion preview,
            Runnable selectionAction
    ) {
        Objects.requireNonNull(skin, "skin cannot be null");
        this.selectionAction = Objects.requireNonNull(
                selectionAction,
                "selection action cannot be null"
        );

        setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"
        ));
        setSize(PACKET_WIDTH, PACKET_HEIGHT);
        pad(4f);

        previewImage = preview == null ? new Image() : new Image(preview);
        previewImage.setScaling(Scaling.fit);
        add(previewImage).size(48f, 60f).padRight(3f);

        Table details = new Table();
        nameLabel = label(skin, plantName, 0.68f);
        nameLabel.setWrap(true);
        details.add(nameLabel).width(77f).height(25f).row();

        costLabel = label(skin, "", 0.66f);
        details.add(costLabel).width(77f).height(17f).row();

        statusLabel = label(skin, "", 0.62f);
        details.add(statusLabel).width(77f).height(18f);
        add(details).size(78f, 62f);

        clickListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (!disposed && state.selectable() && selectionAction != null) {
                    selectionAction.run();
                }
            }
        };
        addListener(clickListener);
        applyState();
    }

    public void update(int sunCost, SeedPacketState.View state) {
        if (disposed) {
            return;
        }
        if (sunCost < 0) {
            throw new IllegalArgumentException("sun cost cannot be negative");
        }
        this.state = Objects.requireNonNull(state, "state cannot be null");
        costLabel.setText(sunCost + " SUN");
        statusLabel.setText(state.statusText());
        applyState();
    }

    private void applyState() {
        Color color = switch (state.availability()) {
            case READY -> READY_COLOR;
            case UNAVAILABLE -> UNAVAILABLE_COLOR;
            case SELECTED -> SELECTED_COLOR;
        };
        setColor(color);
        Color textColor = state.availability()
                == SeedPacketState.Availability.UNAVAILABLE
                ? Color.LIGHT_GRAY
                : Color.DARK_GRAY;
        previewImage.setColor(state.availability()
                == SeedPacketState.Availability.UNAVAILABLE
                ? Color.GRAY
                : Color.WHITE);
        nameLabel.setColor(textColor);
        costLabel.setColor(textColor);
        statusLabel.setColor(state.availability()
                == SeedPacketState.Availability.READY
                ? READY_TEXT_COLOR
                : textColor);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        selectionAction = null;
        setTouchable(Touchable.disabled);
        removeListener(clickListener);
        clearActions();
        clearListeners();
        remove();
        // previewImage points at a shared TextureBank region. It is not owned
        // by this packet and must not be disposed here.
    }

    private static Label label(Skin skin, String text, float fontScale) {
        Label label = new Label(text, skin);
        label.setAlignment(Align.center);
        label.setFontScale(fontScale);
        return label;
    }
}
