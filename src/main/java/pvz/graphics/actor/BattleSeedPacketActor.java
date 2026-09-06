package pvz.graphics.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
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

    private final PlantCompactCardContent content;
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

        setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        setSize(PACKET_WIDTH, PACKET_HEIGHT);
        pad(4f);

        content = new PlantCompactCardContent(
                skin,
                preview,
                plantName,
                48f,
                77f
        );
        content.setTextScales(0.55f, 0.48f, 0.45f);
        add(content).grow();

        clickListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (!disposed
                        && state.selectable()
                        && BattleSeedPacketActor.this.selectionAction != null) {
                    BattleSeedPacketActor.this.selectionAction.run();
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
        content.update(sunCost, state.statusText());
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
        content.setPreviewColor(
                state.availability() == SeedPacketState.Availability.UNAVAILABLE
                        ? Color.GRAY
                        : Color.WHITE
        );
        content.setContentColor(textColor);
        content.setStatusColor(
                state.availability() == SeedPacketState.Availability.READY
                        ? READY_TEXT_COLOR
                        : textColor
        );
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
        // TextureBank regions are shared and are not owned by this packet.
    }
}
